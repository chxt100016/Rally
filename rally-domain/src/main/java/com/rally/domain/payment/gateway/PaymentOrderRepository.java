package com.rally.domain.payment.gateway;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付单读写网关（操作中间件 → Repository 后缀）。
 * 状态推进一律条件更新（where status=期望值）防并发覆盖。
 */
public interface PaymentOrderRepository {

    /** 新增（uk_active_ref 活跃标识位唯一，防同一用户对同一业务重复建活跃单） */
    void saveBatch(List<PaymentOrder> orders);

    /** 按 bizId（out_trade_no）查询 */
    PaymentOrder findByBizId(String bizId);

    /** 按活跃标识位查当前活跃单（PENDING/PAID）；无则返回 null（幂等建单回查用） */
    PaymentOrder findActiveByRef(BizTypeEnum bizType, String refBizId, String payerUserId);

    /** 回写预支付凭证（prepay_id 及其有效期） */
    void updatePrepay(String bizId, String prepayId, LocalDateTime prepayExpireTime);

    /** 某用户的待支付支付单（待处理列表「我有待支付」） */
    List<PaymentOrder> listPendingByPayer(String payerUserId);

    /** 扫描超时未支付订单（仅在配置了超时时使用：status=PENDING && expire_time IS NOT NULL && expire_time<now） */
    List<PaymentOrder> listExpiredPending(LocalDateTime now);

    /** 条件更新为已支付（where status=PENDING），返回是否更新成功 */
    boolean markPaid(String bizId, String transactionId, LocalDateTime payTime);

    /** 条件更新为已关闭（where status=PENDING） */
    boolean close(String bizId);

    /** 条件更新为失败（where status=PENDING） */
    boolean markFailed(String bizId, String reason);
}
