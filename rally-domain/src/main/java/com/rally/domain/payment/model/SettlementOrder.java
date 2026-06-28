package com.rally.domain.payment.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.enums.SettlementStatusEnum;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 分账单聚合根（充血模型，见设计 §15.2）。
 * 状态机：PENDING ──markProcessing──▶ PROCESSING ──markFinished──▶ FINISHED；PROCESSING ──markFailed──▶ FAILED。
 * 与支付单 1:1。
 */
@Getter
public class SettlementOrder {

    private final SettlementOrderData data;

    public SettlementOrder(SettlementOrderData data) {
        this.data = data;
    }

    public String getBizId() {
        return data.getBizId();
    }

    /**
     * 工厂：从已支付的支付单建分账单。shareAmount = baseAmount（手续费不分账，见 §6）；置 PENDING。
     */
    public static SettlementOrder createFrom(PaymentOrder order) {
        order.assertPaid();
        PaymentOrderData po = order.getData();
        SettlementOrderData data = new SettlementOrderData();
        data.setBizId(IdWorker.getIdStr());
        data.setPaymentOrderId(po.getBizId());
        data.setChannel(po.getChannel());
        data.setChannelTransactionId(po.getChannelTransactionId());
        data.setMeetupId(po.getMeetupId());
        data.setPayeeUserId(po.getPayeeUserId());
        data.setPayeeAccount(po.getPayeeAccount());
        data.setShareAmount(po.getBaseAmount());
        data.setStatus(SettlementStatusEnum.PENDING);
        return new SettlementOrder(data);
    }

    /** PENDING → PROCESSING，记渠道分账单号 */
    public void markProcessing(String channelOrderId) {
        Assert.isTrue(data.getStatus() == SettlementStatusEnum.PENDING, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
        data.setStatus(SettlementStatusEnum.PROCESSING);
        data.setChannelOrderId(channelOrderId);
    }

    /** 已 FINISHED 幂等返回；PROCESSING → FINISHED */
    public void markFinished() {
        if (data.getStatus() == SettlementStatusEnum.FINISHED) {
            return;
        }
        Assert.isTrue(data.getStatus() == SettlementStatusEnum.PROCESSING, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
        data.setStatus(SettlementStatusEnum.FINISHED);
        data.setFinishTime(LocalDateTime.now());
    }

    /** PROCESSING → FAILED（可重试） */
    public void markFailed(String reason) {
        Assert.isTrue(data.getStatus() == SettlementStatusEnum.PROCESSING, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
        data.setStatus(SettlementStatusEnum.FAILED);
        data.setFailReason(reason);
    }

    public boolean isProcessing() {
        return data.getStatus() == SettlementStatusEnum.PROCESSING;
    }

    /** FAILED 可重试 */
    public boolean canRetry() {
        return data.getStatus() == SettlementStatusEnum.FAILED;
    }
}
