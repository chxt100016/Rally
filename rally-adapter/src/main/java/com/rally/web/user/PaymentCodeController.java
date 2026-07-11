package com.rally.web.user;

import com.rally.domain.tour.model.Result;
import com.rally.user.PaymentCodeAppService;
import com.rally.user.model.PaymentCodeCmd;
import com.rally.user.model.PaymentCodeDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/payment-code")
public class PaymentCodeController {

    @Resource
    private PaymentCodeAppService paymentCodeAppService;

    @PostMapping("")
    public Result<Void> saveWechatPaymentCode(@Valid @RequestBody PaymentCodeCmd cmd) {
        paymentCodeAppService.saveWechatPaymentCode(cmd);
        return Result.ok();
    }

    @GetMapping("")
    public Result<PaymentCodeDTO> getWechatPaymentCode() {
        return Result.ok(paymentCodeAppService.getWechatPaymentCode());
    }

    @DeleteMapping("")
    public Result<Void> deleteWechatPaymentCode() {
        paymentCodeAppService.deleteWechatPaymentCode();
        return Result.ok();
    }
}
