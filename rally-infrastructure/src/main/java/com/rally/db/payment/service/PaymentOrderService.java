package com.rally.db.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.payment.entity.PaymentOrderPO;
import com.rally.db.payment.mapper.PaymentOrderMapper;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrderService extends ServiceImpl<PaymentOrderMapper, PaymentOrderPO> {
}
