package com.rally.db.meetup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名/审核等待表 PO
 */
@Data
@TableName("rally_meetup_waitlist")
public class WaitlistPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String rallyMeetupId;
    private String userId;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime optTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
