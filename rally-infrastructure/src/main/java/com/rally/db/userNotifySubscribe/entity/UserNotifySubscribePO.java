package com.rally.db.userNotifySubscribe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户订阅通知流水 PO
 */
@Data
@TableName("user_notify_subscribe")
public class UserNotifySubscribePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String userId;
    private String bizType;
    private String refBizId;
    private String noticeScene;
    private String templateId;
    private String status;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
