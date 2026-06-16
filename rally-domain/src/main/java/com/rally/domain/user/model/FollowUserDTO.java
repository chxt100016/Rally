package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 关注/被关注列表项
 */
@Data
@Accessors(chain = true)
public class FollowUserDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    /** NTRP 自评分值 */
    private BigDecimal ntrpScore;
    /** 当前登录用户是否已关注 TA */
    private Boolean isFollowed;
    /** 翻页游标：下一页请求时作为 lastId 传入（取列表最后一条的 cursor） */
    private String cursor;
}
