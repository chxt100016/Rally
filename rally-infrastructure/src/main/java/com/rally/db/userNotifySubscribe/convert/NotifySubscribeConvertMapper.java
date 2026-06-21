package com.rally.db.userNotifySubscribe.convert;

import com.rally.db.userNotifySubscribe.entity.UserNotifySubscribePO;
import com.rally.domain.notify.model.NotifySubscribe;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 订阅通知流水 PO ↔ Domain 转换（枚举与 String 由 MapStruct 按 name 自动转换）
 */
@Mapper
public interface NotifySubscribeConvertMapper {

    NotifySubscribeConvertMapper INSTANCE = Mappers.getMapper(NotifySubscribeConvertMapper.class);

    UserNotifySubscribePO toPO(NotifySubscribe data);

    List<UserNotifySubscribePO> toPOList(List<NotifySubscribe> dataList);

    NotifySubscribe toData(UserNotifySubscribePO po);

    List<NotifySubscribe> toDataList(List<UserNotifySubscribePO> poList);
}
