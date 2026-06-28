package com.rally.job;

import com.rally.domain.payment.enums.PaymentStatusEnum;
import com.rally.domain.payment.gateway.PaymentLogRepository;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentLog;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.payment.SettlementAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付回调漏处理补偿任务（设计 §5.4.1）。
 * 扫 payment_log WHERE log_type=CALLBACK AND process_status=RECEIVED AND create_time<now-N，重放推进。
 * 重放策略：以查单为权威（已支付补 markPaid + 触发分账），原 log 标记 PROCESSED。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.payment_callback_recover.enabled", havingValue = "true")
public class PaymentCallbackRecoverJob {

    /** 收到回调后允许处理的最长时间（分钟）；超过仍 RECEIVED 即视为漏处理 */
    private static final int RECEIVED_TIMEOUT_MINUTES = 5;

    @Resource
    private PaymentLogRepository paymentLogRepository;

    @Resource
    private PaymentOrderRepository paymentOrderRepository;

    @Resource
    private PaymentDomainService paymentDomainService;

    @Resource
    private SettlementAppService settlementAppService;

    @Scheduled(cron = "${job.payment_callback_recover.cron:0 */10 * * * ?}")
    public void scan() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(RECEIVED_TIMEOUT_MINUTES);
        List<PaymentLog> logs = paymentLogRepository.listUnprocessedCallback(before);
        if (logs.isEmpty()) {
            return;
        }
        log.info("回调漏处理补偿扫描: {} 条 RECEIVED 超时", logs.size());
        for (PaymentLog logEntry : logs) {
            try {
                recover(logEntry);
            } catch (Exception e) {
                log.error("回调补偿失败 bizId={}", logEntry.getData().getBizId(), e);
                logEntry.markFailed(e.getMessage());
                paymentLogRepository.update(logEntry);
            }
        }
    }

    private void recover(PaymentLog logEntry) {
        String refId = logEntry.getData().getRefId();
        if (StringUtils.isBlank(refId) || !"ORDER".equals(logEntry.getData().getRefType())) {
            // 分账回调本身不驱动状态，直接标记 PROCESSED
            logEntry.markProcessed();
            paymentLogRepository.update(logEntry);
            return;
        }
        PaymentOrder order = paymentOrderRepository.findByBizId(refId);
        if (order == null) {
            logEntry.markFailed("payment_order_not_found");
            paymentLogRepository.update(logEntry);
            return;
        }
        if (order.isPending()) {
            // 以查单为权威：已支付则补推 + 分账；未支付不关单（保留 PENDING 等下次回调/查单）
            PaymentOrder afterCheck = paymentDomainService.recoverIfPaid(order);
            if (afterCheck.getData().getStatus() == PaymentStatusEnum.PAID) {
                settlementAppService.share(afterCheck);
            }
        }
        logEntry.markProcessed();
        paymentLogRepository.update(logEntry);
    }
}
