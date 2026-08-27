package com.rally.domain.payment.paymentorder;

import java.time.LocalDateTime;

/**
 * {@code payment_order} 唯一写端口。
 *
 * <p>适配器必须用单条 SQL 完成各方法描述的写入。C3-C5 带
 * {@code biz_id + status=PENDING} 条件；C2 只按 {@code biz_id} 普通更新。</p>
 */
public interface PaymentOrderPersistence {

    /** 插入完整 PENDING 状态，并区分 biz_id 与 uk_active_ref 唯一键冲突。 */
    PaymentOrderInsertResult insert(PaymentOrderState state);

    /** C2：按 bizId 普通更新 prepay_id 与 prepay_expire_time，不检查影响行数。 */
    void savePrepay(String bizId, String prepayId, LocalDateTime prepayExpireTime);

    /**
     * 兼容既有导出签名；新实现应使用 {@link #savePrepay(String, String, LocalDateTime)}。
     */
    boolean savePrepayIfPending(String bizId, String prepayId, LocalDateTime prepayExpireTime);

    /** C3：仅在仍为 PENDING 时同时写入 PAID、渠道流水和渠道成交时间。 */
    boolean markPaidIfPending(String bizId, String channelTransactionId, LocalDateTime channelPaidAt);

    /** C4：仅在仍为 PENDING 时同时写入 CLOSED 并清空 active_ref_key。 */
    boolean closeIfPending(String bizId);

    /** C5：仅在仍为 PENDING 时同时写入 FAILED、失败摘要并清空 active_ref_key。 */
    boolean failIfPending(String bizId, String failureSummary);

    /** 兼容既有读取签名；C3-C5 不用它补查条件更新结果。 */
    PaymentOrderState findByBizId(String bizId);
}
