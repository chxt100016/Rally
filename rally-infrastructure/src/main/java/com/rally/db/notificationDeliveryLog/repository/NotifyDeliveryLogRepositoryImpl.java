package com.rally.db.notificationDeliveryLog.repository;

import com.rally.db.notificationDeliveryLog.convert.NotificationDeliveryLogConvertMapper;
import com.rally.db.notificationDeliveryLog.entity.NotificationDeliveryLogPO;
import com.rally.db.notificationDeliveryLog.service.NotificationDeliveryLogDbService;
import com.rally.domain.notify.gateway.NotifyDeliveryLogRepository;
import com.rally.domain.notify.model.NotifyDeliveryLog;
import com.rally.domain.notify.model.NotifyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 通知触达日志网关实现。 */
@Component
@RequiredArgsConstructor
public class NotifyDeliveryLogRepositoryImpl implements NotifyDeliveryLogRepository {

    private static final NotificationDeliveryLogConvertMapper MAPPER = NotificationDeliveryLogConvertMapper.INSTANCE;
    private final NotificationDeliveryLogDbService service;

    @Override
    public boolean tryStart(NotifyDeliveryLog deliveryLog) {
        try {
            return service.save(MAPPER.toPO(deliveryLog));
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public void markResult(String bizId, NotifyResult result) {
        service.lambdaUpdate()
                .eq(NotificationDeliveryLogPO::getBizId, bizId)
                .set(NotificationDeliveryLogPO::getStatus, result.getStatus().name())
                .set(NotificationDeliveryLogPO::getProviderMessageId, result.getProviderMessageId())
                .set(NotificationDeliveryLogPO::getProviderTemplateId, result.getProviderTemplateId())
                .set(NotificationDeliveryLogPO::getErrorCode, result.getErrorCode())
                .set(NotificationDeliveryLogPO::getFailReason, result.getFailReason())
                .set(NotificationDeliveryLogPO::getSendTime, LocalDateTime.now())
                .update();
    }
}
