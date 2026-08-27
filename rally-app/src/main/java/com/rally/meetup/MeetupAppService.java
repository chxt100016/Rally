package com.rally.meetup;

import com.rally.utils.UserContext;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.model.MeetupEditCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.meetup.activity.BuildMeetupEditSummaryActivity;
import com.rally.meetup.activity.CreateOpenMeetupActivity;
import com.rally.meetup.activity.JoinPublisherChatActivity;
import com.rally.meetup.activity.OpenMeetupContext;
import com.rally.meetup.activity.ReviseMeetupActivity;
import com.rally.notify.MeetupNotifyAssembler;
import com.rally.notify.NotificationEventId;
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

    private final NotificationDeliveryService notificationDeliveryService;

    private final MeetupCardPackingService meetupCardPackingService;

    private final CreateOpenMeetupActivity createOpenMeetupActivity;

    private final JoinPublisherChatActivity joinPublisherChatActivity;

    private final ReviseMeetupActivity reviseMeetupActivity;

    private final BuildMeetupEditSummaryActivity buildMeetupEditSummaryActivity;

    /**
     * 发布约球
     */
    @Transactional
    public void publish(MeetupPublishCmd cmd) {
        String userId = UserContext.get();

        // 校验发布资格与内容，建立 OPEN 约球和发布者 JOINED 报名。
        OpenMeetupContext meetupContext = createOpenMeetupActivity.execute(userId, cmd);

        // 与约球及发布者报名保持同一事务，建立发布者的初始聊天成员关系。
        joinPublisherChatActivity.execute(meetupContext.meetupId(), meetupContext.publisherId());

    }

    /**
     * 编辑约球
     */
    @Transactional
    public MeetupVO edit(MeetupEditCmd cmd) {
        String userId = UserContext.get();

        // 校验并保存约球编辑资料；活动内部保留既有校验与映射顺序。
        MeetupData data = reviseMeetupActivity.execute(userId, cmd);

        // 由保存后的同一内存对象形成编辑摘要与背景。
        return buildMeetupEditSummaryActivity.execute(data);
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

        // 4. 提交后异步通知全体已加入参与人（创建人除外）
        dispatchMeetupCancelNotifications(meetupId, userId, meetup, data);
        log.info("约球已关闭: meetupId={}", meetupId);
    }

    /**
     * 约球关闭通知编排。触达服务在当前事务提交后异步执行，
     * 并在发送前复核参与资格；其失败不影响已提交的 CLOSED 事实。
     */
    private void dispatchMeetupCancelNotifications(String meetupId, String creatorId,
                                                    Meetup meetup, MeetupData data) {
        notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.MEETUP_CANCEL, meetupId),
                NotifyBizType.MEETUP, meetupId, NoticeScene.MEETUP_CANCEL,
                meetup.getActiveParticipantIds(creatorId), MeetupNotifyAssembler.meetupCancelData(data),
                uid -> meetupDomainService.shouldNotice(meetupId, uid));
    }

    /**
     * 修改约球价格
     */
    @Transactional
    public void editPrice(MeetupEditPriceCmd cmd) {
        String userId = UserContext.get();
        String meetupId = cmd.getMeetupId();

        // 1. 获取聚合根
        Meetup meetup = meetupDomainService.get(meetupId);

        // 2. 权限校验 + 更新价格
        meetupDomainService.editPrice(userId, meetup, cmd);
    }
}
