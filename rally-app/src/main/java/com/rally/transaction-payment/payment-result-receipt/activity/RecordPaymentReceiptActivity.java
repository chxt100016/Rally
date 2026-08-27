package com.rally.transactionpayment.paymentresultreceipt.activity;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.payment.entity.PaymentLogPO;
import com.rally.db.payment.service.PaymentLogService;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.model.CallbackResult;
import com.rally.domain.payment.receiptlog.CompleteCallbackCommand;
import com.rally.domain.payment.receiptlog.ReceiptLog;
import com.rally.domain.payment.receiptlog.ReceiptLogInsertResult;
import com.rally.domain.payment.receiptlog.ReceiptLogPersistence;
import com.rally.domain.payment.receiptlog.ReceiptLogReference;
import com.rally.domain.payment.receiptlog.ReceiptLogState;
import com.rally.domain.payment.receiptlog.RecordPaymentEventCommand;
import com.rally.domain.payment.service.PaymentChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 业务活动 record-payment-receipt：验真解密微信通知，并记录每次回调的处理结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordPaymentReceiptActivity {

    private final PaymentChannelRouter paymentChannelRouter;
    private final ConfirmPaymentActivity confirmPaymentActivity;
    private final PaymentLogService paymentLogService;

    /**
     * 保留 main 的回调事务边界：验真失败在留痕前被吞掉；业务异常在
     * 事务方法内部捕获，因此异常前已发生的状态可以随本次调用提交。
     *
     * @return true 表示渠道可应答 SUCCESS；false 表示应答 FAIL 并等待微信重试
     */
    @Transactional
    public boolean execute(String body, Map<String, String> headers) {
        PaymentChannelClient client = paymentChannelRouter.route(PayChannelEnum.WECHAT);
        CallbackResult callback;
        try {
            // A1：原样传递原始 body 与全部请求头，交易事件由现有微信 SDK 验签解密。
            callback = client.verifyAndParse(body, headers);
        } catch (Exception e) {
            log.error("支付回调验签解密失败", e);
            return false;
        }

        // A2：每次已解析的通知都以新雪花业务键建立 CALLBACK/RECEIVED。
        // refType 始终保留 ORDER，即使未取得商户单号；rawBody 保留渠道客户端给出的原值。
        ReceiptLogPersistence persistence = new PaymentLogPersistence();
        ReceiptLog receipt = ReceiptLog.record(
                new RecordPaymentEventCommand(
                        PayChannelEnum.WECHAT,
                        PaymentLogTypeEnum.CALLBACK,
                        ReceiptLogReference.paymentOrder(callback.getOutTradeNo()),
                        callback.getDecryptedBody()),
                IdWorker::getIdStr,
                persistence);

        try {
            // A3：仅成功交易且商户单号非空时进入既有确认与业务分流。
            // markPaid 保留已 PAID 回调的幂等抑制及 PENDING 首次付款推进语义。
            if ("TRANSACTION".equals(callback.getCallbackType())
                    && callback.isSuccess()
                    && StringUtils.isNotBlank(callback.getOutTradeNo())) {
                confirmPaymentActivity.execute(
                        callback.getOutTradeNo(), callback.getChannelTransactionId());
            }

            // A4：无需推进与业务处理成功都按 bizId 普通更新为 PROCESSED。
            receipt.completeCallback(
                    new CompleteCallbackCommand(PaymentLogStatusEnum.PROCESSED, null),
                    persistence);
            return true;
        } catch (Exception e) {
            log.error("支付回调处理失败 outTradeNo={}", callback.getOutTradeNo(), e);
            // 保留 main：失败摘要原样使用 getMessage()，不补值、不截断；
            // FAILED 更新若再次抛错则继续向上传播。
            receipt.completeCallback(
                    new CompleteCallbackCommand(PaymentLogStatusEnum.FAILED, e.getMessage()),
                    persistence);
            return false;
        }
    }

    /** 把现有 payment_log MyBatis 服务适配为回执日志聚合的唯一写端口。 */
    private final class PaymentLogPersistence implements ReceiptLogPersistence {

        @Override
        public ReceiptLogInsertResult insert(ReceiptLogState state) {
            PaymentLogPO created = new PaymentLogPO();
            created.setBizId(state.bizId());
            created.setChannel(state.channel().name());
            created.setLogType(state.logType().name());
            created.setRefType(state.reference().refType());
            created.setRefId(state.reference().refId());
            created.setRawBody(state.rawBody());
            created.setProcessStatus(state.processStatus().name());
            created.setRemark(state.remark());
            // 与 main 一致：忽略 save 返回值，持久化异常原样上抛。
            paymentLogService.save(created);
            return ReceiptLogInsertResult.CREATED;
        }

        @Override
        public void updateConclusion(
                String bizId, PaymentLogStatusEnum conclusion, String remark) {
            PaymentLogPO existing = paymentLogService.lambdaQuery()
                    .eq(PaymentLogPO::getBizId, bizId)
                    .one();
            if (existing == null) {
                // 与旧 PaymentLogRepositoryImpl.update 一致：无记录时静默返回。
                return;
            }
            PaymentLogPO update = new PaymentLogPO();
            update.setId(existing.getId());
            update.setProcessStatus(conclusion.name());
            update.setRemark(remark);
            // 普通 updateById，不附加 RECEIVED 条件且忽略影响行数。
            paymentLogService.updateById(update);
        }
    }
}
