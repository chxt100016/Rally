package com.rally.domain.payment.service;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;

/**
 * 支付成功业务通知策略（能力四：通知业务方）。
 * payment 领域确认支付成功后，按 bizType 路由到对应业务方 handler 执行后续动作（发货/推进报名等），
 * payment 领域本身不感知具体业务。新增业务类型只需新增一个实现，payment 代码零改动。
 */
public interface PaymentPaidHandler {

    /** 是否处理该业务类型 */
    boolean supports(BizTypeEnum bizType);

    /** 支付成功后的业务动作（与支付确认同事务，抛异常则整体回滚） */
    void onPaid(PaymentOrder paidOrder);
}
