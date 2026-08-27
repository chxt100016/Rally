package com.rally.domain.payment.receiptlog;

/** C1 插入结果，用于把 uk_biz_id 并发冲突映射为稳定领域错误。 */
public enum ReceiptLogInsertResult {
    CREATED,
    BIZ_ID_CONFLICT
}
