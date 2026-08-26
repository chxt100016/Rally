package com.rally.domain.notify.model;

import com.rally.domain.notify.enums.NotifyDeliveryStatus;
import lombok.Data;

/**
 * 通知发送结果
 */
@Data
public class NotifyResult {

    private NotifyDeliveryStatus status;
    private String providerMessageId;
    private String providerTemplateId;
    private String errorCode;
    private String failReason;

    public static NotifyResult sent(String providerMessageId, String providerTemplateId) {
        NotifyResult result = new NotifyResult();
        result.setStatus(NotifyDeliveryStatus.SENT);
        result.setProviderMessageId(providerMessageId);
        result.setProviderTemplateId(providerTemplateId);
        return result;
    }

    public static NotifyResult failed(String errorCode, String reason, String providerTemplateId) {
        NotifyResult result = new NotifyResult();
        result.setStatus(NotifyDeliveryStatus.FAILED);
        result.setErrorCode(errorCode);
        result.setFailReason(reason);
        result.setProviderTemplateId(providerTemplateId);
        return result;
    }

    public static NotifyResult skipped(String errorCode, String reason, String providerTemplateId) {
        NotifyResult result = new NotifyResult();
        result.setStatus(NotifyDeliveryStatus.SKIPPED);
        result.setErrorCode(errorCode);
        result.setFailReason(reason);
        result.setProviderTemplateId(providerTemplateId);
        return result;
    }
}
