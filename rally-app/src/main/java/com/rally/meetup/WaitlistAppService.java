package com.rally.meetup;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.WaitlistGateway;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.WaitlistData;
import com.rally.domain.meetup.model.WaitlistVO;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 报名、撤回、退出、审批通过/拒绝、审批列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistAppService {

    private final MeetupGateway meetupGateway;
    private final WaitlistGateway waitlistGateway;
    private final UserGateway userGateway;
    private final TennisProfileGateway tennisProfileGateway;

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

        // 2. 状态校验（懒判定）
        MeetupStatusEnum realStatus = lazyStatus(meetup);
        if (realStatus == MeetupStatusEnum.FULL) {
            throw new BusinessException(BizErrorCode.MEETUP_FULL);
        }
        if (realStatus == MeetupStatusEnum.CLOSED) {
            throw new BusinessException(BizErrorCode.MEETUP_CLOSED);
        }
        if (realStatus == MeetupStatusEnum.FINISHED) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }

        // 3. 开始时间校验
        if (meetup.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }

        // 4. 不能报名自己发布的约球
        if (userId.equals(meetup.getCreatorId())) {
            throw new BusinessException(BizErrorCode.CANNOT_JOIN_OWN);
        }

        // 5. 重复报名校验
        WaitlistData existing = waitlistGateway.findActiveByMeetupAndUser(meetupId, userId);
        if (existing != null) {
            throw new BusinessException(BizErrorCode.ALREADY_JOINED);
        }

        // 6. 性别限制校验
        checkGenderLimit(meetup, userId);

        // 7. 信誉分门槛校验
        checkReputationScore(userId);

        // 8. 报名抵冲校验
        checkTimeConflict(userId, meetup.getStartTime(), meetup.getEndTime(), null);

        // 9. 判断是否可以复活已失效的记录
        WaitlistData oldRecord = waitlistGateway.findByMeetupAndUserAny(meetupId, userId);

        if (oldRecord != null && (oldRecord.getStatus() == WaitlistStatusEnum.rejected
                || oldRecord.getStatus() == WaitlistStatusEnum.withdrawn
                || oldRecord.getStatus() == WaitlistStatusEnum.expired)) {
            // 复活旧记录
            waitlistGateway.revive(oldRecord.getBizId(), autoWithdrawAt);
        } else {
            // 新增报名记录
            WaitlistData waitlist = new WaitlistData();
            waitlist.setRallyMeetupId(meetupId);
            waitlist.setUserId(userId);
            waitlist.setExpiresAt(autoWithdrawAt);

            if (meetup.getJoinMode() == com.rally.domain.meetup.enums.JoinModeEnum.DIRECT) {
                // 直接加入
                waitlist.setStatus(WaitlistStatusEnum.approved);
                waitlistGateway.save(waitlist);

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
                waitlist.setStatus(WaitlistStatusEnum.pending);
                waitlistGateway.save(waitlist);

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

        WaitlistData waitlist = waitlistGateway.findActiveByMeetupAndUser(meetupId, userId);
        if (waitlist == null) {
            throw new BusinessException(BizErrorCode.NOT_JOINED);
        }

        if (waitlist.getStatus() != WaitlistStatusEnum.pending) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_PENDING);
        }

        waitlistGateway.updateStatus(waitlist.getBizId(), WaitlistStatusEnum.withdrawn);
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

        // 2. 查询报名记录
        WaitlistData waitlist = waitlistGateway.findActiveByMeetupAndUser(meetupId, userId);
        if (waitlist == null || waitlist.getStatus() != WaitlistStatusEnum.approved) {
            throw new BusinessException(BizErrorCode.NOT_JOINED);
        }

        // 3. 计算是否扣分
        long hoursUntilStart = ChronoUnit.HOURS.between(LocalDateTime.now(), meetup.getStartTime());
        int thresholdHours = SystemConfig.getInt("meetup.quit.penalty_threshold_hours", 6);

        if (hoursUntilStart < thresholdHours) {
            // 扣分
            int penalty = SystemConfig.getInt("meetup.quit.penalty_under_6h", 25);
            // TODO: 调用评分域扣分
            log.info("退出扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
        }

        // 4. 释放名额
        int affected = meetupGateway.decrementPlayers(meetupId);
        if (affected > 0) {
            // 标记 waitlist 为 withdrawn
            waitlistGateway.updateStatus(waitlist.getBizId(), WaitlistStatusEnum.withdrawn);
            log.info("退出成功: userId={}, meetupId={}", userId, meetupId);
        }
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(String waitlistId) {
        String currentUserId = UserContext.get();

        // 1. 查询报名记录
        WaitlistData waitlist = waitlistGateway.findByBizId(waitlistId);
        if (waitlist == null) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_FOUND);
        }

        // 2. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(waitlist.getRallyMeetupId());
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 3. 权限校验：仅创建人可审批
        if (!currentUserId.equals(meetup.getCreatorId())) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 4. 状态校验
        if (waitlist.getStatus() != WaitlistStatusEnum.pending) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_PENDING);
        }

        // 5. 懒判定
        MeetupStatusEnum realStatus = lazyStatus(meetup);
        if (realStatus == MeetupStatusEnum.FINISHED || realStatus == MeetupStatusEnum.CLOSED) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 6. 报名抵冲校验
        checkTimeConflict(waitlist.getUserId(), meetup.getStartTime(), meetup.getEndTime(), null);

        // 7. 更新状态
        waitlistGateway.updateStatus(waitlistId, WaitlistStatusEnum.approved);

        // 8. 原子自增人数
        int affected = meetupGateway.incrementPlayers(meetup.getBizId());
        if (affected == 0) {
            throw new BusinessException(BizErrorCode.MEETUP_FULL);
        }

        // 9. 发送通知
        // TODO: 调用通知域
        log.info("审批通过: waitlistId={}, userId={}", waitlistId, waitlist.getUserId());
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(String waitlistId) {
        String currentUserId = UserContext.get();

        // 1. 查询报名记录
        WaitlistData waitlist = waitlistGateway.findByBizId(waitlistId);
        if (waitlist == null) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_FOUND);
        }

        // 2. 查询约球
        MeetupData meetup = meetupGateway.findByBizId(waitlist.getRallyMeetupId());
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 3. 权限校验
        if (!currentUserId.equals(meetup.getCreatorId())) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 4. 状态校验
        if (waitlist.getStatus() != WaitlistStatusEnum.pending) {
            throw new BusinessException(BizErrorCode.WAITLIST_NOT_PENDING);
        }

        // 5. 更新状态
        waitlistGateway.updateStatus(waitlistId, WaitlistStatusEnum.rejected);

        // 6. 发送通知
        // TODO: 调用通知域
        log.info("审批拒绝: waitlistId={}, userId={}", waitlistId, waitlist.getUserId());
    }

    /**
     * 审批列表
     */
    public List<WaitlistVO> pendingList(String meetupId) {
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
        List<WaitlistData> waitlistList = waitlistGateway.findPendingByMeetupId(meetupId);

        // 4. 转换为 VO
        List<WaitlistVO> voList = new ArrayList<>();
        for (WaitlistData waitlist : waitlistList) {
            WaitlistVO vo = MAPPER.toWaitlistVO(waitlist);

            // 填充报名人信息
            UserData user = userGateway.findByUserId(waitlist.getUserId()).orElse(null);
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatarUrl(user.getAvatarUrl());
            }
            TennisProfileData profile = tennisProfileGateway.findByUserId(waitlist.getUserId()).orElse(null);
            if (profile != null) {
                vo.setNtrpScore(profile.getNtrpScore());
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 懒判定
     */
    private MeetupStatusEnum lazyStatus(MeetupData data) {
        if ((data.getStatus() == MeetupStatusEnum.OPEN || data.getStatus() == MeetupStatusEnum.FULL)
                && data.getEndTime().isBefore(LocalDateTime.now())) {
            return MeetupStatusEnum.FINISHED;
        }
        return data.getStatus();
    }

    /**
     * 性别限制校验
     */
    private void checkGenderLimit(MeetupData meetup, String userId) {
        if (meetup.getGenderLimit() == com.rally.domain.meetup.enums.GenderLimitEnum.ANY) {
            return;
        }

        UserData user = userGateway.findByUserId(userId).orElse(null);
        if (user == null || user.getGender() == null) {
            return;
        }

        String userGender = user.getGender().name().toLowerCase();
        if (meetup.getGenderLimit() == com.rally.domain.meetup.enums.GenderLimitEnum.MALE
                && !"MALE".equals(userGender)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
        if (meetup.getGenderLimit() == com.rally.domain.meetup.enums.GenderLimitEnum.FEMALE
                && !"FEMALE".equals(userGender)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
    }

    /**
     * 信誉分门槛校验
     */
    private void checkReputationScore(String userId) {
        TennisProfileData profile = tennisProfileGateway.findByUserId(userId).orElse(null);
        if (profile != null && profile.getReputationScore() != null) {
            if (profile.getReputationScore().compareTo(new BigDecimal("30")) < 0) {
                throw new BusinessException(BizErrorCode.LOW_REPUTATION_BANNED);
            }
        }
    }

    /**
     * 报名抵冲校验
     */
    private void checkTimeConflict(String userId, LocalDateTime startTime, LocalDateTime endTime,
                                    String excludeMeetupId) {
        int bufferMinutes = SystemConfig.getInt("meetup.conflict.buffer_minutes", 30);
        LocalDateTime conflictStart = startTime.minusMinutes(bufferMinutes);
        LocalDateTime conflictEnd = endTime.plusMinutes(bufferMinutes);

        List<WaitlistData> conflicts = waitlistGateway.findConflict(
                userId, conflictStart, conflictEnd, excludeMeetupId);

        if (!conflicts.isEmpty()) {
            throw new BusinessException(BizErrorCode.TIME_CONFLICT);
        }
    }
}
