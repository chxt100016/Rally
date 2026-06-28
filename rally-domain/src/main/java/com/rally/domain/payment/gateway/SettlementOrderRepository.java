package com.rally.domain.payment.gateway;

import com.rally.domain.payment.model.SettlementOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分账单读写网关（与支付单 1:1，uk_payment_order 幂等）。
 */
public interface SettlementOrderRepository {

    /** 新增分账单 */
    void save(SettlementOrder order);

    /** 按 bizId（out_order_no）查询 */
    SettlementOrder findByBizId(String bizId);

    /** 按支付单号查询（幂等判定：已存在直接返回） */
    SettlementOrder findByPaymentOrderId(String paymentOrderId);

    /** 全部分账中订单（对账 Job 扫描） */
    List<SettlementOrder> listProcessing();

    /** 受益人是否存在分账中订单（接收方淘汰前置校验） */
    boolean hasProcessingByPayee(String payeeUserId);

    /** 条件更新为分账中（where status=PENDING） */
    boolean markProcessing(String bizId, String channelOrderId);

    /** 条件更新为已完成（where status=PROCESSING） */
    boolean markFinished(String bizId, LocalDateTime finishTime);

    /** 条件更新为失败（where status=PROCESSING） */
    boolean markFailed(String bizId, String reason);
}
