package com.rally.web.payment;

import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.domain.payment.model.PrepayCmd;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.tour.model.Result;
import com.rally.payment.PaymentAppService;
import com.rally.payment.PaymentQueryAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 支付接口：取拉起参数 / 查我的待付。支付回调见 {@link com.rally.wechat.payment.WechatPayNotifyController}。
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Resource
    private PaymentAppService paymentAppService;

    @Resource
    private PaymentQueryAppService paymentQueryAppService;

    /**
     * 取支付参数（参与人点支付时调用）
     */
    @PostMapping("/prepay")
    public Result<PrepayResult> prepay(@Valid @RequestBody PrepayCmd cmd) {
        return Result.ok(paymentAppService.prepay(cmd));
    }

    /**
     * 查询当前用户的待支付单
     */
    @GetMapping("/my-pending")
    public Result<List<PaymentOrderSummaryDTO>> myPending() {
        return Result.ok(paymentQueryAppService.myPending());
    }
}
