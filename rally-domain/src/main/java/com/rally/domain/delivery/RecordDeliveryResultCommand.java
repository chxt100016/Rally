package com.rally.domain.delivery;

import com.rally.domain.notify.enums.NotifyDeliveryStatus;

/** C4 将一次渠道调用的终态结果回写触达日志的命令。 */
public record RecordDeliveryResultCommand(
        NotifyDeliveryStatus status,
        String providerMessageId,
        String providerTemplateId,
        String errorCode,
        String failReason) {
}
