package com.rally.db.payment.convert;

import com.rally.db.payment.entity.PaymentLogPO;
import com.rally.db.payment.entity.PaymentOrderPO;
import com.rally.domain.payment.model.PaymentLogData;
import com.rally.domain.payment.model.PaymentOrderData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 支付域 PO ↔ Data 转换（枚举 ↔ String 由 MapStruct 自动按 name 转换）。
 */
@Mapper
public interface PaymentConvertMapper {

    PaymentConvertMapper INSTANCE = Mappers.getMapper(PaymentConvertMapper.class);

    // ==================== PaymentOrder ====================

    PaymentOrderData toOrderData(PaymentOrderPO po);

    List<PaymentOrderData> toOrderDataList(List<PaymentOrderPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    PaymentOrderPO toOrderPO(PaymentOrderData data);

    // ==================== PaymentLog ====================

    PaymentLogData toLogData(PaymentLogPO po);

    List<PaymentLogData> toLogDataList(List<PaymentLogPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    PaymentLogPO toLogPO(PaymentLogData data);
}
