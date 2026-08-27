package com.rally.meetup.activity;

import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 join-approved-participant-chat：为审批通过的申请人建立初始聊天成员关系。
 */
@Component
@RequiredArgsConstructor
public class JoinApprovedParticipantChatActivity {

    private final ChatDomainService chatDomainService;

    public void execute(String meetupId, String approvedUserId) {
        // A1-A2：领域命令先按 refId+userId 查重，再以空已读位置、未读数 0 建立成员。
        chatDomainService.join(meetupId, approvedUserId);

        // A3：正常返回即继续通知编排；重复成员或保存失败由外层审批事务整体回滚。
    }
}
