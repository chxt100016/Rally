package com.rally.job;

import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentDomainService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付超时关单兜底任务（设计 §5.4.1）。
 * 默认 payment.pay_timeout_minutes=0 不超时，故默认 Job 关闭；配置了超时分钟数才启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.payment_timeout.enabled", havingValue = "true")
public class PaymentTimeoutJob {

    @Resource
    private PaymentOrderRepository paymentOrderRepository;

    @Resource
    private PaymentDomainService paymentDomainService;

    @Scheduled(cron = "${job.payment_timeout.cron:0 */5 * * * ?}")
    public void scan() {
        List<PaymentOrder> expired = paymentOrderRepository.listExpiredPending(LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("支付超时关单扫描: {} 单待处理", expired.size());
        for (PaymentOrder order : expired) {
            try {
                paymentDomainService.timeoutCheck(order);
            } catch (Exception e) {
                log.error("超时关单失败 bizId={}", order.getBizId(), e);
            }
        }
    }
}
