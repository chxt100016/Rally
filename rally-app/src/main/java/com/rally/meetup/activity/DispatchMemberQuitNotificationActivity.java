package com.rally.meetup.activity;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.notify.NotificationEventId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务活动 dispatch-member-quit-notification：退出事务提交后尽力通知约球发布者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchMemberQuitNotificationActivity {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationDeliveryService notificationDeliveryService;

    public void execute(MemberQuitNotificationContext context) {
        // A1：报名编号形成稳定退出事件；语义化内容沿用既有微信模板字段和时间格式。
        String eventId = NotificationEventId.of(NoticeScene.MEMBER_QUIT, context.registrationId());

        // 发布者为空时不建立触达任务，也不改变已经完成的退出结果。
        if (context.creatorId() == null || context.creatorId().isBlank()) {
            log.info("成员退出通知缺少发布者，跳过触达: eventId={}, meetupId={}", eventId, context.meetupId());
            return;
        }

        try {
            Map<String, Object> content = memberQuitContent(context);

            // A2-A3：领域触达命令负责在当前退出事务提交后异步取得微信执行权并发送。
            notificationDeliveryService.notify(
                    eventId,
                    NotifyBizType.MEETUP,
                    context.meetupId(),
                    NoticeScene.MEMBER_QUIT,
                    List.of(context.creatorId()),
                    content);
        } catch (Exception e) {
            // A4：通知安排、发送或审计失败均不得反向改变退出终态。
            log.error("安排成员退出通知失败，保留退出结果: eventId={}, meetupId={}",
                    eventId, context.meetupId(), e);
        }
    }

    private Map<String, Object> memberQuitContent(MemberQuitNotificationContext context) {
        Map<String, Object> content = new HashMap<>();
        content.put("activityName", context.meetupName());
        content.put("activityTime", context.startTime().format(TIME_FORMATTER));
        content.put("quitMember", context.quitUserNickname());
        content.put("quitTime", context.quitTime().format(TIME_FORMATTER));
        return content;
    }
}
