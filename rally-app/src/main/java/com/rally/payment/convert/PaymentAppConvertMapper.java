package com.rally.payment.convert;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 支付域 app 层 MapStruct 转换器（出参 DTO）。
 */
@Mapper
public interface PaymentAppConvertMapper {

    PaymentAppConvertMapper INSTANCE = Mappers.getMapper(PaymentAppConvertMapper.class);

    @Mapping(target = "paymentId", source = "data.bizId")
    @Mapping(target = "meetupId", source = "data.meetupId")
    @Mapping(target = "payerUserId", source = "data.payerUserId")
    @Mapping(target = "baseAmount", source = "data.baseAmount")
    @Mapping(target = "feeAmount", source = "data.feeAmount")
    @Mapping(target = "payAmount", source = "data.payAmount")
    @Mapping(target = "status", expression = "java(order.toViewStatus())")
    PaymentOrderSummaryDTO toSummary(PaymentOrder order);

    List<PaymentOrderSummaryDTO> toSummaryList(List<PaymentOrder> orders);
}
