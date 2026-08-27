package com.rally.domain.social.userfollow;

/** C2 解除一个方向的关注关系。 */
public record UnfollowUserCommand(
        String currentUserId,
        String targetUserId) {
}
