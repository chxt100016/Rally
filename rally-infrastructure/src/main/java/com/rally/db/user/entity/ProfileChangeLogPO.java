package com.rally.db.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_profile_change_log")
public class ProfileChangeLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String userId;
    /** ntrp/reputation/credibility/calibration/under_review */
    private String type;
    private BigDecimal beforeValue;
    private BigDecimal afterValue;
    private BigDecimal value;
    /** user/system/review_bad */
    private String reason;
    private String remark;
    private String refId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
