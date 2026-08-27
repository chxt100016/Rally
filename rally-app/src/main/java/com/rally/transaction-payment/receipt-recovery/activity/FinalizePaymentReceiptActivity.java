package com.rally.transactionpayment.receiptrecovery.activity;

import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.model.PaymentLog;
import com.rally.domain.payment.receiptlog.CompleteCallbackCommand;
import com.rally.domain.payment.receiptlog.ReceiptLog;
import com.rally.domain.payment.receiptlog.ReceiptLogInsertResult;
import com.rally.domain.payment.receiptlog.ReceiptLogPersistence;
import com.rally.domain.payment.receiptlog.ReceiptLogReference;
import com.rally.domain.payment.receiptlog.ReceiptLogState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务活动 finalize-payment-receipt：把单条回执恢复结论结束为 PROCESSED 或 FAILED。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalizePaymentReceiptActivity {

    private static final String PAYMENT_ORDER_REFERENCE = "ORDER";

    private final com.rally.domain.payment.gateway.PaymentLogRepository paymentLogRepository;

    /**
     * A1-A3：执行一条恢复链并结束回执。
     *
     * <p>恢复、业务推进或第一次终结的异常都会尝试按原异常消息写 FAILED；该失败
     * 留痕成功后方法正常返回，外层循环可继续下一条。FAILED 更新本身再次抛错时
     * 不做内层吸收，异常直接离开本方法，从而中断当前扫描。</p>
     */
    public void execute(PaymentLog receipt, RecoveryAttempt recoveryAttempt) {
        try {
            execute(recoveryAttempt.recover(receipt));
        } catch (Exception failure) {
            log.error("回调补偿失败 bizId={}", receipt.getData().getBizId(), failure);
            complete(receipt, PaymentLogStatusEnum.FAILED, failure.getMessage());
        }
    }

    /** A1/A2：订单缺失使用固定原因，其余正常短路或推进成功均结束为 PROCESSED。 */
    public void execute(ReconcilePaymentStatusActivity.Result result) {
        if (result.failed()) {
            complete(result.receipt(), PaymentLogStatusEnum.FAILED, result.failureReason());
            return;
        }
        complete(result.receipt(), PaymentLogStatusEnum.PROCESSED, null);
    }

    private void complete(PaymentLog receipt, PaymentLogStatusEnum conclusion, String remark) {
        ReceiptLog callback = restoreForCompletion(receipt);
        callback.completeCallback(
                new CompleteCallbackCommand(conclusion, remark),
                new ExistingPaymentLogPersistence(receipt));
    }

    /**
     * 恢复扫描允许处理历史无效关联。C2 只写 bizId、结论与原因，因此无效关联在
     * 聚合快照中投影为未关联，不会改写数据库中的原始关联字段。
     */
    private ReceiptLog restoreForCompletion(PaymentLog receipt) {
        var data = receipt.getData();
        ReceiptLogReference reference = PAYMENT_ORDER_REFERENCE.equals(data.getRefType())
                ? ReceiptLogReference.paymentOrder(data.getRefId())
                : ReceiptLogReference.none();
        return ReceiptLog.restore(new ReceiptLogState(
                null,
                data.getBizId(),
                data.getChannel(),
                data.getLogType(),
                reference,
                data.getRawBody(),
                data.getProcessStatus(),
                data.getRemark(),
                data.getCreateTime(),
                data.getUpdateTime()));
    }

    /** 单条恢复链；调用方可组合 reconcile 与 advance，异常由本活动统一留痕。 */
    @FunctionalInterface
    public interface RecoveryAttempt {
        ReconcilePaymentStatusActivity.Result recover(PaymentLog receipt) throws Exception;
    }

    /**
     * 适配现有仓储的按 bizId 普通更新：无 RECEIVED 条件、无 CAS，也不检查影响行数。
     */
    private final class ExistingPaymentLogPersistence implements ReceiptLogPersistence {

        private final PaymentLog receipt;

        private ExistingPaymentLogPersistence(PaymentLog receipt) {
            this.receipt = receipt;
        }

        @Override
        public ReceiptLogInsertResult insert(ReceiptLogState state) {
            throw new UnsupportedOperationException("恢复终结不建立支付回执");
        }

        @Override
        public void updateConclusion(
                String bizId, PaymentLogStatusEnum conclusion, String remark) {
            if (conclusion == PaymentLogStatusEnum.PROCESSED) {
                receipt.markProcessed();
            } else {
                receipt.markFailed(remark);
            }
            paymentLogRepository.update(receipt);
        }
    }
}
