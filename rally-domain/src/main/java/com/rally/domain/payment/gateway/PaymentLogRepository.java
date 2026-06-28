package com.rally.domain.payment.gateway;

import com.rally.domain.payment.model.PaymentLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付全链路留痕读写网关（建单/下单/回调，对账与排查）。
 */
public interface PaymentLogRepository {

    /** 新增留痕 */
    void save(PaymentLog log);

    /** 更新留痕（回调处理推进 RECEIVED → PROCESSED/FAILED） */
    void update(PaymentLog log);

    /** 扫描超时未处理的回调留痕（log_type=CALLBACK && process_status=RECEIVED && create_time<before），补偿重放 */
    List<PaymentLog> listUnprocessedCallback(LocalDateTime before);
}
