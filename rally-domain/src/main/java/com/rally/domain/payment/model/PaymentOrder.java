package com.rally.domain.payment.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentStatusEnum;
import com.rally.domain.payment.enums.PaymentViewStatus;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 支付单聚合根（充血模型，见设计 §15.1）。
 * 状态机：PENDING ──markPaid──▶ PAID；PENDING ──close──▶ CLOSED；PENDING ──markFailed──▶ FAILED。
 * 聚合根只管状态 + 不变量 + 状态机，不碰 Repository/Client/事务。
 */
@Getter
public class PaymentOrder {

    private final PaymentOrderData data;

    public PaymentOrder(PaymentOrderData data) {
        this.data = data;
    }

    public String getBizId() {
        return data.getBizId();
    }

    /**
     * 工厂：算手续费 + 生成 bizId(out_trade_no) + 置 PENDING + 算 expireTime。金额一律后端算。
     * @param timeoutMinutes 超时分钟数，<=0 则不超时（expireTime=null）
     * @param payeeAccount 收款受益人渠道账号（微信 openid），冗余进单方便分账直接取
     */
    public static PaymentOrder create(PayChannelEnum channel, BizTypeEnum bizType, String batchId, String meetupId, String payerUserId, String payeeUserId, String payeeAccount, int baseAmount, BigDecimal feeRate, int timeoutMinutes) {
        int feeAmount = calcFee(baseAmount, feeRate);
        PaymentOrderData data = new PaymentOrderData();
        data.setBizId(IdWorker.getIdStr());
        data.setChannel(channel);
        data.setBizType(bizType);
        data.setCollectionBatchId(batchId);
        data.setMeetupId(meetupId);
        data.setPayerUserId(payerUserId);
        data.setPayeeUserId(payeeUserId);
        data.setPayeeAccount(payeeAccount);
        data.setBaseAmount(baseAmount);
        data.setFeeAmount(feeAmount);
        data.setPayAmount(baseAmount + feeAmount);
        data.setStatus(PaymentStatusEnum.PENDING);
        data.setExpireTime(timeoutMinutes > 0 ? LocalDateTime.now().plusMinutes(timeoutMinutes) : null);
        return new PaymentOrder(data);
    }

    /** ceil(base * rate) */
    private static int calcFee(int base, BigDecimal rate) {
        return BigDecimal.valueOf(base).multiply(rate).setScale(0, RoundingMode.CEILING).intValueExact();
    }

    // ======================== 状态机（幂等 + 条件流转） ========================

    /** 支付成功：已 PAID 直接返回（幂等）；非 PENDING 抛状态非法；置 PAID + 记渠道流水号 + 支付时间 */
    public void markPaid(String transactionId) {
        if (data.getStatus() == PaymentStatusEnum.PAID) {
            return;
        }
        Assert.isTrue(data.getStatus() == PaymentStatusEnum.PENDING, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
        data.setStatus(PaymentStatusEnum.PAID);
        data.setChannelTransactionId(transactionId);
        data.setPayTime(LocalDateTime.now());
    }

    /** 关闭：已 CLOSED 直接返回；仅 PENDING 可关，否则抛（已 PAID 不可关） */
    public void close() {
        if (data.getStatus() == PaymentStatusEnum.CLOSED) {
            return;
        }
        Assert.isTrue(data.getStatus() == PaymentStatusEnum.PENDING, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
        data.setStatus(PaymentStatusEnum.CLOSED);
    }

    /** 失败：仅 PENDING → FAILED */
    public void markFailed(String reason) {
        Assert.isTrue(data.getStatus() == PaymentStatusEnum.PENDING, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
        data.setStatus(PaymentStatusEnum.FAILED);
        data.setDescription(reason);
    }

    // ======================== 断言 / 判定 ========================

    /** 分账前置：必须已支付 */
    public void assertPaid() {
        Assert.isTrue(data.getStatus() == PaymentStatusEnum.PAID, BizErrorCode.PAYMENT_STATUS_ILLEGAL);
    }

    /** prepay 前置：当前用户必须是付款人 */
    public void assertPayer(String userId) {
        Assert.isTrue(data.getPayerUserId().equals(userId), BizErrorCode.PAYMENT_NOT_PAYER);
    }

    public boolean isPending() {
        return data.getStatus() == PaymentStatusEnum.PENDING;
    }

    /** 是否已超时（expireTime != null && expireTime < now；默认 null=永不超时） */
    public boolean isExpired() {
        return data.getExpireTime() != null && data.getExpireTime().isBefore(LocalDateTime.now());
    }

    /** 内部态 → 对外视图态（避免内部枚举外泄，见 §5.5） */
    public PaymentViewStatus toViewStatus() {
        return switch (data.getStatus()) {
            case PENDING -> PaymentViewStatus.UNPAID;
            case PAID -> PaymentViewStatus.PAID;
            case CLOSED, FAILED -> PaymentViewStatus.CLOSED;
        };
    }
}
