package com.rally.meetup;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.model.RegistrationVO;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 报名/注册服务：报名、撤回、退出、审批通过/拒绝、审批列表
 * 记录所有参与者：创建者、等待审批、已通过等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationAppService {

    private final MeetupGateway meetupGateway;
    private final RegistrationGateway registrationGateway;
    private final UserGateway userGateway;
    private final TennisProfileGateway tennisProfileGateway;
    private final MeetupDomainService meetupDomainService;

    private static final MeetupAppConvertMapper MAPPER = MeetupAppConvertMapper.INSTANCE;

    /**
     * 报名
     */
    @Transactional
    public void join(String meetupId, LocalDateTime autoWithdrawAt) {
        String userId = UserContext.get();

        // 1. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(meetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 领域校验：状态、时间、创建人（委托聚合根）
        new Meetup(meetup).assertCanJoin(userId);

        // 3. 重复报名校验
        RegistrationData existing = registrationGateway.findActiveByMeetupAndUser(meetupId, userId);
        if (existing != null) {
            throw new BusinessException(BizErrorCode.ALREADY_JOINED);
        }

        // 4. 性别限制校验（委托聚合根，app 层负责查询用户数据）
        UserData user = userGateway.findByUserId(userId).orElse(null);
        if (user != null && user.getGender() != null) {
            new Meetup(meetup).checkGenderLimit(user.getGender().name());
        }

        // 5. 信誉分门槛校验（委托领域服务，app 层负责查询档案数据）
        TennisProfileData profile = tennisProfileGateway.findByUserId(userId).orElse(null);
        if (profile != null) {
            meetupDomainService.checkReputationScore(profile.getReputationScore());
        }

        // 6. 报名抵冲校验（委托领域服务）
        meetupDomainService.checkTimeConflict(userId, meetup.getStartTime(), meetup.getEndTime(), null);

        // 7. 判断是否可以复活已失效的记录
        RegistrationData oldRecord = registrationGateway.findByMeetupAndUserAny(meetupId, userId);

        if (Meetup.canRevive(oldRecord)) {
            // 复活旧记录
            registrationGateway.revive(oldRecord.getBizId(), autoWithdrawAt);
        } else {
            // 新增报名记录
            RegistrationData registration = new RegistrationData();
            registration.setRallyMeetupId(meetupId);
            registration.setUserId(userId);
            registration.setExpiresAt(autoWithdrawAt);

            if (meetup.getJoinMode() == com.rally.domain.meetup.enums.JoinModeEnum.DIRECT) {
                // 直接加入
                registration.setStatus(WaitlistStatusEnum.approved);
                registrationGateway.save(registration);

                // 原子自增人数
                int affected = meetupGateway.incrementPlayers(meetupId);
                if (affected == 0) {
                    throw new BusinessException(BizErrorCode.MEETUP_FULL);
                }

                // 发送报名通过通知
                // TODO: 调用通知域
                log.info("报名通过: userId={}, meetupId={}", userId, meetupId);
            } else {
                // 审批模式
                registration.setStatus(WaitlistStatusEnum.pending);
                registrationGateway.save(registration);

                // 发送新申请通知给发布者
                // TODO: 调用通知域
                log.info("新申请: userId={}, meetupId={}", userId, meetupId);
            }
        }
    }

    /**
     * 撤回（仅 pending 可撤）
     */
    @Transactional
    public void withdraw(String meetupId) {
        String userId = UserContext.get();

        RegistrationData registration = registrationGateway.findActiveByMeetupAndUser(meetupId, userId);
        if (registration == null) {
            throw new BusinessException(BizErrorCode.NOT_JOINED);
        }

        // 状态校验委托聚合根
        if (!Meetup.canWithdraw(registration)) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_PENDING);
        }

        registrationGateway.updateStatus(registration.getBizId(), WaitlistStatusEnum.withdrawn);
        log.info("撤回报名: userId={}, meetupId={}", userId, meetupId);
    }

    /**
     * 退出（已加入）
     */
    @Transactional
    public void quit(String meetupId) {
        String userId = UserContext.get();

        // 1. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(meetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 查询报名记录并校验状态（委托聚合根）
        RegistrationData registration = registrationGateway.findActiveByMeetupAndUser(meetupId, userId);
        if (registration == null || !Meetup.canQuit(registration)) {
            throw new BusinessException(BizErrorCode.NOT_JOINED);
        }

        // 3. 计算扣分（委托领域服务）
        int penalty = meetupDomainService.calculateQuitPenalty(meetup, userId);
        if (penalty > 0) {
            // TODO: 调用评分域扣分
            log.info("退出扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
        }

        // 4. 释放名额
        int affected = meetupGateway.decrementPlayers(meetupId);
        if (affected > 0) {
            // 标记 registration 为 withdrawn
            registrationGateway.updateStatus(registration.getBizId(), WaitlistStatusEnum.withdrawn);
            log.info("退出成功: userId={}, meetupId={}", userId, meetupId);
        }
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(String registrationId) {
        String currentUserId = UserContext.get();

        // 1. 查询报名记录
        RegistrationData registration = registrationGateway.findByBizId(registrationId);
        if (registration == null) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_FOUND);
        }

        // 2. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(registration.getRallyMeetupId());
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 3. 权限校验（委托聚合根）
        if (!new Meetup(meetup).isCreator(currentUserId)) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 4. 状态校验（委托聚合根）
        if (!Meetup.canReview(registration)) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_PENDING);
        }

        // 5. 约球状态校验（委托聚合根懒判定）
        if (!new Meetup(meetup).isActive()) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 6. 报名抵冲校验（委托领域服务）
        meetupDomainService.checkTimeConflict(registration.getUserId(), meetup.getStartTime(),
                meetup.getEndTime(), null);

        // 7. 更新状态
        registrationGateway.updateStatus(registrationId, WaitlistStatusEnum.approved);

        // 8. 原子自增人数
        int affected = meetupGateway.incrementPlayers(meetup.getBizId());
        if (affected == 0) {
            throw new BusinessException(BizErrorCode.MEETUP_FULL);
        }

        // 9. 发送通知
        // TODO: 调用通知域
        log.info("审批通过: registrationId={}, userId={}", registrationId, registration.getUserId());
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(String registrationId) {
        String currentUserId = UserContext.get();

        // 1. 查询报名记录
        RegistrationData registration = registrationGateway.findByBizId(registrationId);
        if (registration == null) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_FOUND);
        }

        // 2. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(registration.getRallyMeetupId());
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 3. 权限校验（委托聚合根）
        if (!new Meetup(meetup).isCreator(currentUserId)) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 4. 状态校验（委托聚合根）
        if (!Meetup.canReview(registration)) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_PENDING);
        }

        // 5. 更新状态
        registrationGateway.updateStatus(registrationId, WaitlistStatusEnum.rejected);

        // 6. 发送通知
        // TODO: 调用通知域
        log.info("审批拒绝: registrationId={}, userId={}", registrationId, registration.getUserId());
    }

    /**
     * 审批列表
     */
    public List<RegistrationVO> pendingList(String meetupId) {
        String currentUserId = UserContext.get();

        // 1. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(meetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 权限校验
        if (!currentUserId.equals(meetup.getCreatorId())) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 3. 查询待审批列表
        List<RegistrationData> registrationList = registrationGateway.findPendingByMeetupId(meetupId);

        // 4. 转换为 VO
        List<RegistrationVO> voList = new ArrayList<>();
        for (RegistrationData registration : registrationList) {
            RegistrationVO vo = MAPPER.toRegistrationVO(registration);

            // 填充报名人信息
            UserData user = userGateway.findByUserId(registration.getUserId()).orElse(null);
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatarUrl(user.getAvatarUrl());
            }
            TennisProfileData profile = tennisProfileGateway.findByUserId(registration.getUserId()).orElse(null);
            if (profile != null) {
                vo.setNtrpScore(profile.getNtrpScore());
            }

            voList.add(vo);
        }

        return voList;
    }
}
