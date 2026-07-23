package com.rally.payment;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.gateway.AccountRepository;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.PaymentLogRepository;
import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.CallbackResult;
import com.rally.domain.payment.model.PaymentLog;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayCmd;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.payment.service.PaymentChannelRouter;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.domain.tournament.service.TournamentPaymentService;
import com.rally.domain.utils.Assert;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 支付应用服务：取支付参数 + 处理支付回调（设计 §5.1 注 / §5.2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAppService {

    private final PaymentDomainService paymentDomainService;
    private final PaymentChannelRouter channelRouter;
    private final PaymentLogRepository paymentLogRepository;
    private final SettlementAppService settlementAppService;
    private final AccountRepository accountRepository;
    private final TournamentPaymentService tournamentPaymentService;

    /**
     * 取拉起参数：参与人点支付时调用。openid 由登录态 user→openid 反查。
     */
    public PrepayResult prepay(PrepayCmd cmd) {
        String userId = UserContext.get();
        String openid = accountRepository.findIdentifierByUser(userId, ChannelEnum.WECHAT_MINIAPP);
        Assert.notBlank(openid, BizErrorCode.WECHAT_AUTH_FAILED);
        return paymentDomainService.prepay(cmd.getPaymentId(), userId, openid);
    }

    /**
     * 支付异步回调处理：验签解密 + 留痕 + 推进状态 + 触发分账。
     * 微信要求按规范返回应答；外层 Controller 负责按 success/failure 写响应体。
     *
     * @return true 处理成功；false 处理失败需告知微信重试
     */
    @Transactional
    public boolean handlePayCallback(String body, Map<String, String> headers) {
        PaymentChannelClient client = channelRouter.route(PayChannelEnum.WECHAT);
        CallbackResult callback;
        try {
            callback = client.verifyAndParse(body, headers);
        } catch (Exception e) {
            log.error("微信支付回调验签解密失败", e);
            return false;
        }

        // 留痕（落 RECEIVED，处理成功后转 PROCESSED）
        String refType = "TRANSACTION".equals(callback.getCallbackType()) ? "ORDER" : "SETTLEMENT";
        PaymentLog logEntry = PaymentLog.callback(PayChannelEnum.WECHAT, refType, callback.getOutTradeNo(), callback.getDecryptedBody());
        paymentLogRepository.save(logEntry);

        try {
            if ("TRANSACTION".equals(callback.getCallbackType()) && callback.isSuccess() && StringUtils.isNotBlank(callback.getOutTradeNo())) {
                // 支付成功 → 推进支付单 → 按业务类型分流（活动收款触发分账 / 赛事报名费推进 entry+席位）
                PaymentOrder paid = paymentDomainService.markPaid(callback.getOutTradeNo(), callback.getChannelTransactionId());
                if (paid.getData().getBizType() == BizTypeEnum.TOURNAMENT_ENTRY_FEE) {
                    tournamentPaymentService.advanceOnPaid(paid);
                } else {
                    settlementAppService.share(paid);
                }
            } else if ("PROFIT_SHARE".equals(callback.getCallbackType())) {
                // 分账动账通知：业务上不依赖此回调推进（最终态以查询为权威，§5.3），仅留痕
                log.info("收到分账动账通知 outOrderNo={}", callback.getOutTradeNo());
            }
            logEntry.markProcessed();
            paymentLogRepository.update(logEntry);
            return true;
        } catch (Exception e) {
            log.error("支付回调处理失败 outTradeNo={}", callback.getOutTradeNo(), e);
            logEntry.markFailed(e.getMessage());
            paymentLogRepository.update(logEntry);
            return false;
        }
    }
}
