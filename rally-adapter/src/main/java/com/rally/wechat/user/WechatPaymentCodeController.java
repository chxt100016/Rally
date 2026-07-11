package com.rally.wechat.user;

import com.rally.web.user.PaymentCodeController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/user/payment-code")
public class WechatPaymentCodeController extends PaymentCodeController {
}
