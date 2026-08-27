package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.ActionStateEnum;
import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 业务活动 query-meetup-chat-unread：只读查询约球群聊未读数。
 */
@Component
@RequiredArgsConstructor
public class QueryMeetupChatUnreadActivity {

    private static final Set<ActionStateEnum> CHAT_ENABLED_STATES = EnumSet.of(
            ActionStateEnum.JOINED,
            ActionStateEnum.ONGOING_JOINED,
            ActionStateEnum.OWNER_EDITABLE,
            ActionStateEnum.OWNER_EDIT_LOCKED,
            ActionStateEnum.FINISHED_JOINED,
            ActionStateEnum.FINISHED_REVIEWED,
            ActionStateEnum.CLOSED_JOINED
    );

    private final ChatDomainService chatDomainService;

    public Integer execute(String meetupId, String currentUserId, ActionStateEnum actionState) {
        // A1：不可进入群聊的状态不访问聊天表。
        if (!CHAT_ENABLED_STATES.contains(actionState)) {
            return null;
        }

        // A2/A3：成员记录存在时返回冗余未读数；否则统计该约球全部消息。
        // A4：领域服务的该查询无任何已读位置或成员记录写入。
        return chatDomainService.getUnreadCount(meetupId, currentUserId);
    }
}
