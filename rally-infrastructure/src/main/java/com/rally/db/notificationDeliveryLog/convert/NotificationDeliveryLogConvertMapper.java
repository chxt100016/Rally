package com.rally.db.notificationDeliveryLog.convert;

import com.rally.db.notificationDeliveryLog.entity.NotificationDeliveryLogPO;
import com.rally.domain.notify.model.NotifyDeliveryLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/** 触达日志 PO 与领域模型转换。 */
@Mapper
public interface NotificationDeliveryLogConvertMapper {

    NotificationDeliveryLogConvertMapper INSTANCE = Mappers.getMapper(NotificationDeliveryLogConvertMapper.class);

    NotificationDeliveryLogPO toPO(NotifyDeliveryLog data);
}
