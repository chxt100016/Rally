package com.rally.domain.notify.model;

import lombok.Data;

/**
 * 通知发送结果
 */
@Data
public class NotifyResult {

    private boolean success;
    private String failReason;

    public static NotifyResult ok() {
        NotifyResult result = new NotifyResult();
        result.setSuccess(true);
        return result;
    }

    public static NotifyResult fail(String reason) {
        NotifyResult result = new NotifyResult();
        result.setSuccess(false);
        result.setFailReason(reason);
        return result;
    }
}
