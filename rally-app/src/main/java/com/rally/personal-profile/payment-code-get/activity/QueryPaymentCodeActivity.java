package com.rally.personalprofile.paymentcodeget.activity;

import com.rally.domain.user.enums.UserExtKeyEnum;
import com.rally.domain.user.model.UserExtData;
import com.rally.domain.user.service.UserExtDomainService;
import com.rally.user.convert.PaymentCodeAppConvertMapper;
import com.rally.user.model.PaymentCodeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-payment-code：读取本人微信收款码并生成可选签名地址。
 */
@Component
@RequiredArgsConstructor
public class QueryPaymentCodeActivity {

    private static final PaymentCodeAppConvertMapper MAPPER = PaymentCodeAppConvertMapper.INSTANCE;

    private final UserExtDomainService userExtDomainService;

    public PaymentCodeDTO execute(String userId) {
        // A1 只按当前用户和历史固定键查询，不额外校验账户或档案。
        UserExtData data = userExtDomainService.get(userId, UserExtKeyEnum.PAYMENT_CODE.getKey());

        // A2 沿用转换器：无记录返回 null，extValue 原样交付，仅非空白值构造一小时签名地址。
        return MAPPER.toDTO(data);
    }
}
