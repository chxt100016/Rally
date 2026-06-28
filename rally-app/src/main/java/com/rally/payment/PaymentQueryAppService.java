package com.rally.payment;

import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.domain.payment.service.PaymentQueryDomainService;
import com.rally.payment.convert.PaymentAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付读模型应用服务（待处理列表「我有待支付」等）。
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryAppService {

    private final PaymentQueryDomainService paymentQueryDomainService;

    /** 当前用户的全部待支付单 */
    public List<PaymentOrderSummaryDTO> myPending() {
        String userId = UserContext.get();
        return PaymentAppConvertMapper.INSTANCE.toSummaryList(paymentQueryDomainService.myPending(userId));
    }
}
