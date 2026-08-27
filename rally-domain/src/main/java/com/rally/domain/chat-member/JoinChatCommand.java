package com.rally.domain.meetup.chatmember;

import java.time.LocalDateTime;

/** C1 加入频道命令。 */
public record JoinChatCommand(String userId, LocalDateTime joinedAt) {
}
