package com.rally.db.notificationDeliveryLog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 通知触达日志持久化对象。 */
@Data
@TableName("notification_delivery_log")
public class NotificationDeliveryLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String eventId;
    private String bizType;
    private String refBizId;
    private String noticeScene;
    private String recipientId;
    private String channel;
    private String providerTemplateId;
    private String status;
    private String providerMessageId;
    private String errorCode;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
