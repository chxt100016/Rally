package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 球员主页聚合视图
 */
@Data
@Accessors(chain = true)
public class PlayerHomeDTO {

    /** 基础用户信息 */
    private MyProfileUserDTO user;

    /** 关注统计 */
    private PlayerHomeStatsDTO stats;

    /** 约球信息 */
    private PlayerHomeMeetupDTO meetup;

    /** 评价信息 */
    private MyProfileReviewDTO review;

    /** 等级信息 */
    private PlayerHomeLevelDTO level;

    /** 评分信息 */
    private PlayerHomeScoreDTO score;

    /** 视频信息 */
    private MyProfileVideoDTO video;
}
