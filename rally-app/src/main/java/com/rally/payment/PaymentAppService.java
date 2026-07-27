package com.rally.payment;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.payment.convert.PaymentAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 支付应用服务：仅负责前端轮询查支付状态 + 回调透传。
 * 取拉起参数、验签推进等支付能力全部下沉 {@link PaymentDomainService}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAppService {

    private final PaymentDomainService paymentDomainService;

    /**
     * 轮询支付状态（前端支付确认中轮询）：
     * 以查单为权威，已支付则补 markPaid（业务分流由 PaymentPaidNotifier 自动完成）；未支付原样返回。
     */
    @Transactional
    public PaymentOrderSummaryDTO syncPayStatus(String paymentId) {
        String userId = UserContext.get();
        PaymentOrder order = paymentDomainService.load(paymentId);
        order.assertPayer(userId);
        if (order.isPending()) {
            order = paymentDomainService.recoverIfPaid(order);
        }
        return PaymentAppConvertMapper.INSTANCE.toSummary(order);
    }

    /**
     * 支付异步回调透传：验签解密 + 留痕 + 状态推进全部在领域层，Controller 按返回值写应答体。
     *
     * @return true 处理成功；false 需告知渠道重试
     */
    public boolean handlePayCallback(String body, Map<String, String> headers) {
        return paymentDomainService.handleCallback(body, headers);
    }
}
