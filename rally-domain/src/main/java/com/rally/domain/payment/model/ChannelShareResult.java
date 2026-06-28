package com.rally.domain.payment.model;

import lombok.Data;

/**
 * 渠道分账结果（发起分账 / 查询分账共用）。
 */
@Data
public class ChannelShareResult {
    private String outOrderNo;
    private String channelOrderId;
    /** 是否已分账完成 */
    private boolean finished;
    /** 是否失败 */
    private boolean failed;
    /** 渠道原始分账状态 */
    private String shareState;
    private String failReason;
}
