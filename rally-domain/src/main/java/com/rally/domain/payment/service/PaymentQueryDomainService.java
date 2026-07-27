package com.rally.domain.payment.service;

import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付读模型领域服务。
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryDomainService {

    private final PaymentOrderRepository paymentOrderRepository;

    /** 待处理列表"我有待支付" */
    public List<PaymentOrder> myPending(String userId) {
        return paymentOrderRepository.listPendingByPayer(userId);
    }
}
