package com.rally.db.notificationDeliveryLog.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.notificationDeliveryLog.entity.NotificationDeliveryLogPO;
import com.rally.db.notificationDeliveryLog.mapper.NotificationDeliveryLogMapper;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryLogDbService
        extends ServiceImpl<NotificationDeliveryLogMapper, NotificationDeliveryLogPO> {
}
