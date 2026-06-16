package com.rally.domain.user.model;

import lombok.Data;

/**
 * 关注关系领域数据
 */
@Data
public class UserFollowData {
    private String bizId;
    /** 关注人 user_id */
    private String followerId;
    /** 被关注人 user_id */
    private String followingId;
}
