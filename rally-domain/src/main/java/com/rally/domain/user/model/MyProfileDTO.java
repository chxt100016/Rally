package com.rally.domain.user.model;

import lombok.Data;

/**
 * 我的档案聚合视图（新版）
 */
@Data
public class MyProfileDTO {

    /** 约球信息 */
    private MyProfileMeetupDTO meetup;

    /** 评价信息 */
    private MyProfileReviewDTO review;

    /** 等级信息 */
    private MyProfileLevelDTO level;

    /** 评分信息 */
    private MyProfileScoreDTO score;

    /** 基础用户信息 */
    private MyProfileUserDTO user;

    /** 视频信息 */
    private MyProfileVideoDTO video;
}
