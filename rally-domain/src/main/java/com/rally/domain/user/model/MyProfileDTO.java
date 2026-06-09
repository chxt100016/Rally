package com.rally.domain.user.model;

import com.rally.domain.user.enums.ProfileStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 我的档案聚合视图（新版）
 */
@Data
@Accessors(chain = true)
public class MyProfileDTO {

    /** 文档状态 */
    private ProfileStatusEnum status;

    /** 基础用户信息 */
    private MyProfileUserDTO user;

    private MyProfileMeetupDTO meetup;


    /* 四格快捷入口 */
    /** 等级信息 */
    private MyProfileLevelDTO level;

    /** 评分信息 */
    private MyProfileScoreDTO score;

    /** 评价信息 */
    private MyProfileReviewDTO review;

    /** 视频信息 */
    private MyProfileVideoDTO video;


}
