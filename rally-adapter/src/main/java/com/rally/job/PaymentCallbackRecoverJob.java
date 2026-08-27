package com.rally.job;

import com.rally.domain.payment.model.PaymentLog;
import com.rally.transactionpayment.receiptrecovery.activity.FinalizePaymentReceiptActivity;
import com.rally.transactionpayment.receiptrecovery.activity.ReconcilePaymentStatusActivity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付回调漏处理补偿任务（设计 §5.4.1）。
 * 扫 payment_log WHERE log_type=CALLBACK AND process_status=RECEIVED AND create_time<now-N，重放推进。
 * 重放策略：以查单为权威（已支付补 markPaid），原 log 标记 PROCESSED。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.payment_callback_recover.enabled", havingValue = "true")
public class PaymentCallbackRecoverJob {

    /** 收到回调后允许处理的最长时间（分钟）；超过仍 RECEIVED 即视为漏处理 */
    @Resource
    private ReconcilePaymentStatusActivity reconcilePaymentStatusActivity;

    @Resource
    private com.rally.transactionpayment.receiptrecovery.activity.AdvancePaidBusinessActivity advancePaidBusinessActivity;

    @Resource
    private FinalizePaymentReceiptActivity finalizePaymentReceiptActivity;

    @Scheduled(cron = "${job.payment_callback_recover.cron:0 */10 * * * ?}")
    public void scan() {
        List<PaymentLog> logs = reconcilePaymentStatusActivity.scan();
        if (logs.isEmpty()) {
            return;
        }
        log.info("回调漏处理补偿扫描: {} 条 RECEIVED 超时", logs.size());
        for (PaymentLog logEntry : logs) {
            finalizePaymentReceiptActivity.execute(logEntry,
                    receipt -> advancePaidBusinessActivity.execute(
                            reconcilePaymentStatusActivity.execute(receipt)));
        }
    }
}
