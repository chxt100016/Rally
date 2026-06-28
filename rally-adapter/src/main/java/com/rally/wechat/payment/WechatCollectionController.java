package com.rally.wechat.payment;

import com.rally.web.payment.CollectionController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/payment/collection")
public class WechatCollectionController extends CollectionController {
}
