package com.rally.domain.social.userfollow;

/** 由关注人和被关注人组成的单向关注值对象。 */
public record UserFollowDirection(
        String followerId,
        String followingId) {
}
