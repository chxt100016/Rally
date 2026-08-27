package com.rally.personalprofile.paymentcodesave.activity;

import com.rally.domain.user.service.UserExtDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 upsert-payment-code-record：新增或覆盖本人唯一收款码资料。
 */
@Component
@RequiredArgsConstructor
public class UpsertPaymentCodeRecordActivity {

    private static final String PAYMENT_CODE_KEY = "wechat_payment_code";

    private final UserExtDomainService userExtDomainService;

    public void execute(String userId, String key) {
        // A1-A3：固定扩展键；C1 负责领域校验，仓储在每次保存时生成新 bizId，
        // 按 userId + extKey 查询后插入，或沿用自增 id 整体更新以替换 bizId 与值。
        userExtDomainService.save(userId, PAYMENT_CODE_KEY, key);
    }
}
