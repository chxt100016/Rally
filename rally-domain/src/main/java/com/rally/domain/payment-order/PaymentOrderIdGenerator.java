package com.rally.domain.payment.paymentorder;

/** C1 的支付业务编号生成端口；实现应生成可作为渠道商户单号的雪花编号。 */
@FunctionalInterface
public interface PaymentOrderIdGenerator {
    String nextBizId();
}
