package com.rally.personalprofile.paymentcodedelete.activity;

import com.rally.domain.user.service.UserExtDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 remove-payment-code-record：二次确认并删除本人收款码扩展资料。
 */
@Component
@RequiredArgsConstructor
public class RemovePaymentCodeRecordActivity {

    private static final String PAYMENT_CODE_KEY = "wechat_payment_code";

    private final UserExtDomainService userExtDomainService;

    public void execute(String userId) {
        // A1/A2 由 C2 命令边界完成二次读取与组合键删除；不比较首读快照，也不检查影响行数。
        userExtDomainService.delete(userId, PAYMENT_CODE_KEY);
    }
}
