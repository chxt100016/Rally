package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 join-direct-participant-chat：为直接加入的报名人建立初始聊天成员关系。
 */
@Component
@RequiredArgsConstructor
public class JoinDirectParticipantChatActivity {

    private final ChatDomainService chatDomainService;

    public void execute(String meetupId, String userId, RegistrationStatusEnum registrationStatus) {
        // A1：待审批报名不进入群聊，保持既有跳过语义。
        if (RegistrationStatusEnum.JOINED != registrationStatus) {
            return;
        }

        // A2-A3：领域命令按 refId+userId 查重，并以空已读位置、未读数 0 建立成员。
        chatDomainService.join(meetupId, userId);

        // A4：命令正常返回即表示加入完成；异常由报名外层事务整体回滚。
    }
}
