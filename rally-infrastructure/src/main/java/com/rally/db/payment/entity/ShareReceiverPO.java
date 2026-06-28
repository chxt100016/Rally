package com.rally.db.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分账接收方账本 PO
 */
@Data
@TableName("payment_share_receiver")
public class ShareReceiverPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String channel;
    private String userId;
    private String accountType;
    private String account;
    private String bindStatus;
    private LocalDateTime boundTime;
    private LocalDateTime lastShareTime;
    private LocalDateTime unbindTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
