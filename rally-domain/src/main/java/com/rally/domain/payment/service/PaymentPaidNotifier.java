package com.rally.domain.payment.service;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付成功通知分发器：按 bizType 路由到匹配的 {@link PaymentPaidHandler}。
 * 无匹配 handler 时仅记日志不报错（如纯查询类支付、后续新增未接业务）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPaidNotifier {

    private final List<PaymentPaidHandler> handlers;

    public void notifyPaid(PaymentOrder paidOrder) {
        BizTypeEnum bizType = paidOrder.getData().getBizType();
        for (PaymentPaidHandler handler : handlers) {
            if (handler.supports(bizType)) {
                handler.onPaid(paidOrder);
                return;
            }
        }
        log.warn("支付成功无匹配业务处理器 bizType={}, bizId={}", bizType, paidOrder.getBizId());
    }
}
