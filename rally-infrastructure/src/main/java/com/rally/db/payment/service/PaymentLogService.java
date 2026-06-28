package com.rally.db.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.payment.entity.PaymentLogPO;
import com.rally.db.payment.mapper.PaymentLogMapper;
import org.springframework.stereotype.Service;

@Service
public class PaymentLogService extends ServiceImpl<PaymentLogMapper, PaymentLogPO> {
}
