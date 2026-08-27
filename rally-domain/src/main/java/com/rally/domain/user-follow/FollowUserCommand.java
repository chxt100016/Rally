package com.rally.domain.social.userfollow;

/** C1 关注用户；目标存在性由调用方在进入聚合前查定。 */
public record FollowUserCommand(
        String currentUserId,
        String targetUserId,
        boolean targetExists) {
}
