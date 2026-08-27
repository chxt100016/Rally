package com.rally.personalprofile.paymentcodedelete.activity;

import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.user.model.UserExtData;
import com.rally.domain.user.service.UserExtDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 delete-payment-code-image：删除本人收款码资料首读快照指向的七牛文件。
 */
@Component
@RequiredArgsConstructor
public class DeletePaymentCodeImageActivity {

    private static final String PAYMENT_CODE_KEY = "wechat_payment_code";

    private final UserExtDomainService userExtDomainService;
    private final QiniuClient qiniuClient;

    public void execute(String userId) {
        // A1 首读无资料时不在此报错，仍交给下游删除记录活动处理。
        UserExtData paymentCode = userExtDomainService.get(userId, PAYMENT_CODE_KEY);

        // A2 只以 null 判断是否调用七牛；空字符串也原样删除，612 由现有客户端容错。
        if (paymentCode != null && paymentCode.getExtValue() != null) {
            qiniuClient.deleteFile(paymentCode.getExtValue());
        }
    }
}
