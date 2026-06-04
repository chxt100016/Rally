package com.rally.db.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价竖表 PO
 */
@Data
@TableName("rally_review")
public class ReviewPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String rallyMeetupId;
    private String fromUserId;
    private String toUserId;
    /** 评价维度：ntrp_vote / attendance / tag */
    private String reviewType;
    /** 评价值 */
    private String reviewValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
