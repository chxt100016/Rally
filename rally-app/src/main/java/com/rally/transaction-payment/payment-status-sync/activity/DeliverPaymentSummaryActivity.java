package com.rally.transactionpayment.paymentstatussync.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.domain.utils.Assert;
import com.rally.payment.convert.PaymentAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 deliver-payment-summary：从本地最新支付单交付既有对外摘要。
 */
@Component
@RequiredArgsConstructor
public class DeliverPaymentSummaryActivity {

    private final PaymentOrderRepository paymentOrderRepository;

    /**
     * 按已校验的支付单号重读本地最终状态，并保持 main 的 DTO 与状态映射语义。
     *
     * <p>本活动是纯查询，不访问支付渠道，也不交付渠道流水、预支付信息、
     * 时间或内部业务类型。</p>
     */
    public PaymentOrderSummaryDTO execute(String paymentId) {
        // A1：无论前置活动是否推进过，都以 payment_order 的最新记录为返回依据。
        PaymentOrder latestOrder = paymentOrderRepository.findByBizId(paymentId);
        Assert.notNull(latestOrder, BizErrorCode.OPERATION_FAILED);

        // A2-A3：复用 main 的转换器，保持金额字段和既有 DTO 不变；聚合映射
        // PENDING→UNPAID、PAID→PAID、CLOSED/FAILED→CLOSED。
        return PaymentAppConvertMapper.INSTANCE.toSummary(latestOrder);
    }
}
