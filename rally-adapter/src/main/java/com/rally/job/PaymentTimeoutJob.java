package com.rally.job;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.transactionpayment.timeoutorderclose.activity.CloseExpiredPaymentActivity;
import com.rally.transactionpayment.timeoutorderclose.activity.ReconcileExpiredPaymentActivity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付超时关单兜底任务（设计 §5.4.1）。
 * 可选的数据清理兜底；支付入口自身会惰性处理超时订单，不依赖本任务开启。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.payment_timeout.enabled", havingValue = "true")
public class PaymentTimeoutJob {

    @Resource
    private ReconcileExpiredPaymentActivity reconcileExpiredPaymentActivity;

    @Resource
    private com.rally.transactionpayment.timeoutorderclose.activity.AdvancePaidBusinessActivity advancePaidBusinessActivity;

    @Resource
    private CloseExpiredPaymentActivity closeExpiredPaymentActivity;

    @Scheduled(cron = "${job.payment_timeout.cron:0 */5 * * * ?}")
    public void scan() {
        List<PaymentOrder> expired = reconcileExpiredPaymentActivity.scan();
        if (expired.isEmpty()) {
            return;
        }
        log.info("支付超时关单扫描: {} 单待处理", expired.size());
        for (PaymentOrder order : expired) {
            try {
                ReconcileExpiredPaymentActivity.Result result = reconcileExpiredPaymentActivity.execute(order);
                if (result.paid()) {
                    advancePaidBusinessActivity.execute(result);
                } else {
                    closeExpiredPaymentActivity.execute(result);
                }
            } catch (Exception e) {
                log.error("超时关单失败 bizId={}", order.getBizId(), e);
            }
        }
    }
}
