package com.rally.web.payment;

import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.domain.tour.model.Result;
import com.rally.payment.PaymentAppService;
import com.rally.payment.PaymentQueryAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 支付接口：查我的待付 / 轮询支付状态。支付回调见 {@link com.rally.wechat.payment.WechatPayNotifyController}。
 * 取拉起参数已内聚到各业务模块（如赛事报名 /tournament/entry/pay），不再提供独立 prepay 接口。
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Resource
    private PaymentAppService paymentAppService;

    @Resource
    private PaymentQueryAppService paymentQueryAppService;

    /**
     * 查询当前用户的待支付单
     */
    @GetMapping("/my-pending")
    public Result<List<PaymentOrderSummaryDTO>> myPending() {
        return Result.ok(paymentQueryAppService.myPending());
    }

    /**
     * 手动同步支付状态（本地开发无公网回调兜底用查单补偿；生产环境正常路径依赖回调，不需要调此接口）
     */
    @PostMapping("/sync-status")
    public Result<PaymentOrderSummaryDTO> syncStatus(@RequestParam("paymentId") String paymentId) {
        return Result.ok(paymentAppService.syncPayStatus(paymentId));
    }
}
