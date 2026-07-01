package com.rally.meetup;

import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import com.rally.domain.court.service.CourtDomainService;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.model.MeetupEditCmd;
import com.rally.domain.meetup.service.MeetupPolicy;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotifySubscribeService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import com.rally.notify.MeetupNotifyAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 约球写流程编排：发布、编辑、关闭
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupAppService {

    private final MeetupDomainService meetupDomainService;

    private final MeetupPolicy meetupPolicy;

    private final CourtDomainService courtDomainService;

    private final ChatDomainService chatDomainService;

    private final NotifySubscribeService notifySubscribeService;

    private final UserProfileDomainService userProfileDomainService;

    /**
     * 发布约球
     */
    @Transactional
    public void publish(MeetupPublishCmd cmd) {
        String userId = UserContext.get();
        UserProfile userProfile = this.userProfileDomainService.get(userId);
        userProfile.assertCompleted();

        // 0. TEXT/MAP 模式下，通过 courtId 查询球场数据覆盖前端数据
        overrideCourtDataIfNeeded(cmd);

        // 1. 校验
        meetupPolicy.assertPublish(userId, cmd);

        // 2. 构建 MeetupData 并持久化（含创建者自动报名）
        String meetupId = meetupDomainService.save(userId, cmd);

        // 加入群聊
        chatDomainService.join(meetupId, userId);

        // 创建人订阅授权建额度（需审批活动前端可含 PENDING_APPROVAL）
        notifySubscribeService.grant(userId, NotifyBizType.MEETUP, meetupId, MeetupNotifyAssembler.parseScenes(cmd.getAcceptedNoticeScenes()));

        // 3. 球场查找或创建（200m 范围内合并）
        courtDomainService.findOrCreate(cmd.getCourtName(), cmd.getCourtAddress(), cmd.getCourtLng(), cmd.getCourtLat(), cmd.getCityCode(), cmd.getDistrictCode(), null);
    }

    /**
     * 编辑约球
     */
    @Transactional
    public MeetupVO edit(MeetupEditCmd cmd) {
        String meetupId = cmd.getMeetupId();
        String userId = UserContext.get();

        // 0. TEXT/MAP 模式下，通过 courtId 查询球场数据覆盖前端数据
        overrideCourtDataIfNeeded(cmd);

        // 1. 获取聚合根
        Meetup meetup = meetupDomainService.get(meetupId);
        MeetupData data = meetup.getData();

        // 2. 编辑校验
        meetupPolicy.assertEdit(meetup, cmd);

        // 3. 更新字段 + 落库
        meetupDomainService.edit(userId, meetup, cmd);

        // 4. 返回详情
        return MeetupAppConvertMapper.INSTANCE.toMeetupVO(data);
    }

    /**
     * TEXT/MAP 模式下，通过 courtId 查询球场库数据覆盖前端传来的地址数据
     */
    private void overrideCourtDataIfNeeded(MeetupPublishCmd cmd) {
        String mode = cmd.getCourtSelectMode();
        String courtId = cmd.getCourtId();

        // 只有 TEXT 或 MAP 模式且有 courtId 时才覆盖
        if (("TEXT".equals(mode) || "MAP".equals(mode)) && courtId != null && !courtId.trim().isEmpty()) {
            CourtData court = courtDomainService.getByBizId(courtId);
            if (court != null) {
                // 用球场库的数据覆盖前端传来的数据
                cmd.setCourtName(court.getName());
                cmd.setCourtAddress(court.getAddress());
                cmd.setCourtLng(court.getLng());
                cmd.setCourtLat(court.getLat());
                // 如果球场库有区县信息，也覆盖
                if (court.getDistrictCode() != null && !court.getDistrictCode().trim().isEmpty()) {
                    cmd.setDistrictCode(court.getDistrictCode());
                }
                log.info("TEXT/MAP模式，使用球场库数据覆盖：courtId={}, name={}, address={}",
                    courtId, court.getName(), court.getAddress());
            } else {
                log.warn("TEXT/MAP模式但未找到球场库数据：courtId={}", courtId);
            }
        }
    }

    /**
     * 关闭约球
     */
    @Transactional
    public void close(String meetupId) {
        String userId = UserContext.get();

        // 1. 查询聚合根
        Meetup meetup = meetupDomainService.get(meetupId);
        MeetupData data = meetup.getData();

        // 2. 权限和状态校验 + 更新状态
        meetupDomainService.close(userId, meetup);

        // 3. 阶梯扣分（如果有人报名）
        if (data.getCurrentPlayers() > 1) {
            int penalty24h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_24H_OUT.getKey());
            int penalty12h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_12_24H.getKey());
            int penalty6h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_6_12H.getKey());
            int penaltyUnder6h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_UNDER_6H.getKey());
            int penalty = meetupDomainService.calculateCancelPenalty(
                    data.getStartTime(), penalty24h, penalty12h, penalty6h, penaltyUnder6h);
            if (penalty > 0) {
                // TODO: 调用评分域扣分（交叉引用 04）
                log.info("发布者关闭约球扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
            }
        }

        // 4. 发送取消通知给全体已加入参与人（创建人除外）
        notifySubscribeService.notify(NotifyBizType.MEETUP, meetupId, NoticeScene.MEETUP_CANCEL, meetup.getActiveParticipantIds(userId), MeetupNotifyAssembler.meetupCancelData(data), uid -> meetupDomainService.shouldNotice(meetupId, uid));
        log.info("约球已关闭: meetupId={}", meetupId);
    }
}
