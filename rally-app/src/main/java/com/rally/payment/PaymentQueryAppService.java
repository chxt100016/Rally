package com.rally.payment;

import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.transactionpayment.pendingordersquery.activity.DeliverPendingOrdersActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付读模型应用服务（待处理列表「我有待支付」等）。
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryAppService {

    private final DeliverPendingOrdersActivity deliverPendingOrdersActivity;

    /** 当前用户的全部待支付单 */
    public List<PaymentOrderSummaryDTO> myPending() {
        return deliverPendingOrdersActivity.execute();
    }
}
