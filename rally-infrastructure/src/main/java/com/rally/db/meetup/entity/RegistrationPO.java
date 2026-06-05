package com.rally.db.meetup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名/注册表 PO（记录所有参与者：创建者、等待审批、已通过等）
 */
@Data
@TableName("rally_meetup_registration")
public class RegistrationPO {
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
