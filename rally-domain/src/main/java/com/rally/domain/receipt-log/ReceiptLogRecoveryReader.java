package com.rally.domain.payment.receiptlog;

import java.time.LocalDateTime;
import java.util.List;

/** CALLBACK/RECEIVED 恢复扫描的只读端口。 */
public interface ReceiptLogRecoveryReader {

    /**
     * 按 id 正序分页读取 create_time 不晚于等待阈值的待处理回调。
     * afterId 首屏可为空，limit 必须为正数；读取不得改变日志状态。
     */
    List<ReceiptLogState> findReceivedCallbacksBefore(
            LocalDateTime waitingThreshold, Long afterId, int limit);
}
