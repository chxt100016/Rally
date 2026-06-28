package com.rally.payment;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.SettlementOrder;
import com.rally.domain.payment.service.SettlementDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分账应用服务（设计 §5.3）。
 * 支付回调成功后被调用：建分账单 + 调微信分账（领域层编排）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementAppService {

    private final SettlementDomainService settlementDomainService;

    /**
     * 对单笔已支付的支付单发起分账。回调路径同步触发，失败不影响支付回调返回成功（兜底由对账 Job 重试）。
     */
    @Transactional
    public SettlementOrder share(PaymentOrder paidOrder) {
        try {
            return settlementDomainService.share(paidOrder);
        } catch (Exception e) {
            log.error("分账失败 paymentOrderId={}, 留待 SettlementReconcileJob 兜底", paidOrder.getBizId(), e);
            return null;
        }
    }
}
