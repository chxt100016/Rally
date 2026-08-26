package com.rally.domain.notify.enums;

/** 单个业务通知事件对单个接收人、单个渠道的触达状态。 */
public enum NotifyDeliveryStatus {
    SENDING,
    SENT,
    FAILED,
    SKIPPED
}
