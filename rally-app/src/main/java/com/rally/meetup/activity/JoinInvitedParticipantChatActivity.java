package com.rally.meetup.activity;

import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 join-invited-participant-chat：为被邀请人建立初始聊天成员关系。
 */
@Component
@RequiredArgsConstructor
public class JoinInvitedParticipantChatActivity {

    private final ChatDomainService chatDomainService;

    public void execute(String meetupId, String inviteeUserId) {
        // A1-A2：领域命令先检查 refId+userId，再生成雪花编号并以未读数 0 建立成员。
        chatDomainService.join(meetupId, inviteeUserId);

        // A3：领域命令正常返回即表示群聊加入完成；异常沿用外层邀请事务整体回滚。
    }
}
