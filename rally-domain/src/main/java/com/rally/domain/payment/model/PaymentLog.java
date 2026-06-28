package com.rally.domain.payment.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;
import lombok.Getter;

/**
 * 支付全链路留痕（见设计 §15.4，非聚合根）。
 * COLLECT/PREPAY 落库即 PROCESSED（纯留痕）；CALLBACK 落 RECEIVED 待处理。
 */
@Getter
public class PaymentLog {

    private final PaymentLogData data;

    public PaymentLog(PaymentLogData data) {
        this.data = data;
    }

    /** 发起人发起收款建单留痕，落库即 PROCESSED */
    public static PaymentLog collect(PayChannelEnum channel, String refId, String rawBody) {
        return build(channel, PaymentLogTypeEnum.COLLECT, "ORDER", refId, rawBody, PaymentLogStatusEnum.PROCESSED);
    }

    /** 参与人每次前端发起支付（下单）留痕，落库即 PROCESSED */
    public static PaymentLog prepay(PayChannelEnum channel, String refId, String rawBody) {
        return build(channel, PaymentLogTypeEnum.PREPAY, "ORDER", refId, rawBody, PaymentLogStatusEnum.PROCESSED);
    }

    /** 渠道回调留痕（支付/分账），落 RECEIVED 待处理 */
    public static PaymentLog callback(PayChannelEnum channel, String refType, String refId, String rawBody) {
        return build(channel, PaymentLogTypeEnum.CALLBACK, refType, refId, rawBody, PaymentLogStatusEnum.RECEIVED);
    }

    private static PaymentLog build(PayChannelEnum channel, PaymentLogTypeEnum logType, String refType, String refId, String rawBody, PaymentLogStatusEnum status) {
        PaymentLogData data = new PaymentLogData();
        data.setBizId(IdWorker.getIdStr());
        data.setChannel(channel);
        data.setLogType(logType);
        data.setRefType(refType);
        data.setRefId(refId);
        data.setRawBody(rawBody);
        data.setProcessStatus(status);
        return new PaymentLog(data);
    }

    /** RECEIVED → PROCESSED（仅 CALLBACK 有推进语义） */
    public void markProcessed() {
        data.setProcessStatus(PaymentLogStatusEnum.PROCESSED);
    }

    public void markFailed(String reason) {
        data.setProcessStatus(PaymentLogStatusEnum.FAILED);
        data.setRemark(reason);
    }
}
