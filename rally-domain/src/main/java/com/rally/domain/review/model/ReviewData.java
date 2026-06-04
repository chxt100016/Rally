package com.rally.domain.review.model;

import com.rally.domain.review.enums.ReviewTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价领域数据对象（单条评价记录 = 一个维度值）
 */
@Data
public class ReviewData {
    /** 业务唯一 ID */
    private String bizId;
    /** 关联约球 biz_id */
    private String rallyMeetupId;
    /** 评价人 user_id */
    private String fromUserId;
    /** 被评价人 user_id */
    private String toUserId;
    /** 评价维度 */
    private ReviewTypeEnum reviewType;
    /** 评价值 */
    private String reviewValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
