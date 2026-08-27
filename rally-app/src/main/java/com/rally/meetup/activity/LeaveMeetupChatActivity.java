package com.rally.meetup.activity;

import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 leave-meetup-chat：移除退出用户的群聊成员关系并返回当前昵称。
 */
@Component
@RequiredArgsConstructor
public class LeaveMeetupChatActivity {

    private final ChatDomainService chatDomainService;

    private final UserProfileDomainService userProfileDomainService;

    public LeaveMeetupChatContext execute(String meetupId, String userId) {
        // A1：领域命令按 refId+userId 直接物理删除；缺失关系时幂等成功。
        chatDomainService.quit(meetupId, userId);

        // A2：删除成员后读取当前用户资料；缺失用户时沿用既有 TOKEN_INVALID 断言。
        UserProfile quitUserProfile = userProfileDomainService.get(userId);

        // A3：只返回外层提交后通知所需的最新昵称；事务边界仍由退出编排维持。
        return new LeaveMeetupChatContext(quitUserProfile.getUser().getNickname());
    }
}
