package com.rally.db.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付全链路留痕 PO
 */
@Data
@TableName("payment_log")
public class PaymentLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String channel;
    private String logType;
    private String refType;
    private String refId;
    private String rawBody;
    private String processStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
