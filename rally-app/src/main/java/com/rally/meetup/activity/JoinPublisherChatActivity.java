package com.rally.meetup.activity;

import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 join-publisher-chat：为约球发布者建立初始聊天成员关系。
 */
@Component
@RequiredArgsConstructor
public class JoinPublisherChatActivity {

    private final ChatDomainService chatDomainService;

    public void execute(String meetupId, String publisherId) {
        // A1-A2：领域命令按 refId+userId 查重，并以空已读位置、空阅读时间和未读数 0 建立成员。
        chatDomainService.join(meetupId, publisherId);

        // A3：命令正常返回即表示写入完成；异常由发布约球的外层事务整体回滚。
    }
}
