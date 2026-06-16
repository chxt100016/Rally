package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 球员主页关注统计
 */
@Data
@Accessors(chain = true)
public class PlayerHomeStatsDTO {
    /** 被关注数（粉丝数） */
    private Long followerCount;
    /** 关注数 */
    private Long followingCount;
    /** 当前登录用户是否已关注该球员 */
    private Boolean isFollowed;
}
