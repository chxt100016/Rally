package com.rally.domain.payment.model;

import com.rally.domain.payment.enums.CollectionStateEnum;
import com.rally.domain.payment.enums.PaymentViewStatus;
import lombok.Data;

import java.util.Map;

/**
 * 约球详情页支付视图子对象（见设计 §5.5/§5.6）。
 * 由 app 层详情编排组装到 {@code MeetupDetailDTO.payment}，约球域零改动。
 */
@Data
public class MeetupPaymentViewDTO {
    /** 发起人·收款入口态：仅发起人且活动已结束时非 null */
    private CollectionStateEnum collectionState;
    /** 参与人·我的支付态：当前用户为应付人时非 null */
    private PaymentViewStatus myPaymentStatus;
    /** 参与人列表·逐人支付态（payerUserId -> 视图态），发起人看列表收款进度 */
    private Map<String, PaymentViewStatus> participantStatus;
}
