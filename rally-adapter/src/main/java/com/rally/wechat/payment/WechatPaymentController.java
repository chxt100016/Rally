package com.rally.wechat.payment;

import com.rally.web.payment.PaymentController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/payment")
public class WechatPaymentController extends PaymentController {
}
