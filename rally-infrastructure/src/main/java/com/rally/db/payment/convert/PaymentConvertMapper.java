package com.rally.db.payment.convert;

import com.rally.db.payment.entity.PaymentLogPO;
import com.rally.db.payment.entity.PaymentOrderPO;
import com.rally.db.payment.entity.SettlementOrderPO;
import com.rally.db.payment.entity.ShareReceiverPO;
import com.rally.domain.payment.model.PaymentLogData;
import com.rally.domain.payment.model.PaymentOrderData;
import com.rally.domain.payment.model.SettlementOrderData;
import com.rally.domain.payment.model.ShareReceiverData;
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

    // ==================== SettlementOrder ====================

    SettlementOrderData toSettlementData(SettlementOrderPO po);

    List<SettlementOrderData> toSettlementDataList(List<SettlementOrderPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SettlementOrderPO toSettlementPO(SettlementOrderData data);

    // ==================== ShareReceiver ====================

    ShareReceiverData toReceiverData(ShareReceiverPO po);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ShareReceiverPO toReceiverPO(ShareReceiverData data);

    // ==================== PaymentLog ====================

    PaymentLogData toLogData(PaymentLogPO po);

    List<PaymentLogData> toLogDataList(List<PaymentLogPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    PaymentLogPO toLogPO(PaymentLogData data);
}
