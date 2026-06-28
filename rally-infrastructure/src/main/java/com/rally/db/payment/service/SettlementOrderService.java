package com.rally.db.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.payment.entity.SettlementOrderPO;
import com.rally.db.payment.mapper.SettlementOrderMapper;
import org.springframework.stereotype.Service;

@Service
public class SettlementOrderService extends ServiceImpl<SettlementOrderMapper, SettlementOrderPO> {
}
