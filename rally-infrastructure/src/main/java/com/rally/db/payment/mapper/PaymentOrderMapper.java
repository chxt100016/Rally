package com.rally.db.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.payment.entity.PaymentOrderPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderPO> {
}
