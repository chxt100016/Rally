package com.rally.domain.payment.receiptlog;

/** C1 的日志业务编号生成端口；实现应返回雪花编号。 */
@FunctionalInterface
public interface ReceiptLogIdGenerator {

    String nextBizId();
}
