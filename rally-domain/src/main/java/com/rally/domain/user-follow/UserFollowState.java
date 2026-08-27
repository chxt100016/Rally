package com.rally.domain.social.userfollow;

import java.time.LocalDateTime;

/** 与 {@code user_follow} 一行对应的不可变关注状态。 */
public record UserFollowState(
        Long id,
        String bizId,
        String followerId,
        String followingId,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static UserFollowState newlyCreated(
            String bizId, UserFollowDirection direction) {
        return new UserFollowState(
                null,
                bizId,
                direction.followerId(),
                direction.followingId(),
                null,
                null);
    }

    public UserFollowDirection direction() {
        return new UserFollowDirection(followerId, followingId);
    }
}
