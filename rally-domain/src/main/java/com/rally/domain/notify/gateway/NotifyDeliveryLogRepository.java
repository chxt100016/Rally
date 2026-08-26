package com.rally.domain.notify.gateway;

import com.rally.domain.notify.model.NotifyDeliveryLog;
import com.rally.domain.notify.model.NotifyResult;

/** 触达日志读写网关。 */
public interface NotifyDeliveryLogRepository {

    /**
     * 尝试创建 SENDING 记录。eventId + recipientId + channel 重复时返回 false。
     */
    boolean tryStart(NotifyDeliveryLog deliveryLog);

    /** 回写渠道触达结果。 */
    void markResult(String bizId, NotifyResult result);
}
