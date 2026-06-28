package com.rally.domain.payment.model;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.ShareReceiverStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分账接收方账本领域数据对象（与 payment_share_receiver 同构）
 */
@Data
public class ShareReceiverData {
    private String bizId;
    private PayChannelEnum channel;
    private String userId;
    /** 接收方类型，对齐微信 receiver.type（PERSONAL_OPENID 个人） */
    private String accountType;
    private String account;
    private ShareReceiverStatusEnum bindStatus;
    private LocalDateTime boundTime;
    private LocalDateTime lastShareTime;
    private LocalDateTime unbindTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
