package com.rally.domain.payment.paymentorder;

/** C1 插入结果，用于把两个数据库唯一键冲突映射到对应不变量。 */
public enum PaymentOrderInsertResult {
    CREATED,
    BIZ_ID_CONFLICT,
    ACTIVE_REF_CONFLICT
}
