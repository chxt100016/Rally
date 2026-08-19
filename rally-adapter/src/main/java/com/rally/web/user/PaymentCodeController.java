package com.rally.web.user;

import com.rally.domain.tour.model.Result;
import com.rally.user.PaymentCodeAppService;
import com.rally.user.model.PaymentCodeCmd;
import com.rally.user.model.PaymentCodeDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/user/payment-code", "/wechat/user/payment-code"})
public class PaymentCodeController {

    @Resource
    private PaymentCodeAppService paymentCodeAppService;

    @PostMapping("")
    public Result<Void> savePaymentCode(@Valid @RequestBody PaymentCodeCmd cmd) {
        paymentCodeAppService.savePaymentCode(cmd);
        return Result.ok();
    }

    @GetMapping("")
    public Result<PaymentCodeDTO> getPaymentCode() {
        return Result.ok(paymentCodeAppService.getPaymentCode());
    }

    @DeleteMapping("")
    public Result<Void> deletePaymentCode() {
        paymentCodeAppService.deletePaymentCode();
        return Result.ok();
    }
}
