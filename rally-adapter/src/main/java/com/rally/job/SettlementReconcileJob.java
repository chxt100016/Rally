package com.rally.job;

import com.rally.domain.payment.gateway.SettlementOrderRepository;
import com.rally.domain.payment.model.SettlementOrder;
import com.rally.domain.payment.service.SettlementDomainService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分账对账任务（设计 §5.4.1 / §5.3）。
 * 分账请求异步且无可靠回调，最终态以主动查询为权威：扫 status=PROCESSING → queryProfitShare → FINISHED/FAILED。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.settlement_reconcile.enabled", havingValue = "true")
public class SettlementReconcileJob {

    @Resource
    private SettlementOrderRepository settlementOrderRepository;

    @Resource
    private SettlementDomainService settlementDomainService;

    @Scheduled(cron = "${job.settlement_reconcile.cron:0 */2 * * * ?}")
    public void scan() {
        List<SettlementOrder> list = settlementOrderRepository.listProcessing();
        if (list.isEmpty()) {
            return;
        }
        log.info("分账对账扫描: {} 单 PROCESSING", list.size());
        for (SettlementOrder order : list) {
            try {
                settlementDomainService.reconcile(order);
            } catch (Exception e) {
                log.error("分账对账失败 bizId={}", order.getBizId(), e);
            }
        }
    }
}
