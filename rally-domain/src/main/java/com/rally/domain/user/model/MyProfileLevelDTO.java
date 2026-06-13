package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 我的档案 - 等级信息
 */
@Data
@Accessors(chain = true)
public class MyProfileLevelDTO {

    /** NTRP 评分 */
    private String ntrpScore;

    private String subTitle;

    private String noticeTitle;

    private String noticeContent;

    private String noticeInfo;

    private boolean canModify;
}
