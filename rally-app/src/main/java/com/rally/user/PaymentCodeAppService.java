package com.rally.user;

import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.user.enums.UserExtKeyEnum;
import com.rally.domain.user.model.UserExtData;
import com.rally.domain.user.service.UserExtDomainService;
import com.rally.user.convert.PaymentCodeAppConvertMapper;
import com.rally.user.model.PaymentCodeCmd;
import com.rally.user.model.PaymentCodeDTO;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCodeAppService {

    @Resource
    private UserExtDomainService userExtDomainService;

    @Resource
    private QiniuClient qiniuClient;

    private static final PaymentCodeAppConvertMapper MAPPER = PaymentCodeAppConvertMapper.INSTANCE;

    @Transactional
    public void savePaymentCode(PaymentCodeCmd cmd) {
        String userId = UserContext.get();
        userExtDomainService.save(userId, UserExtKeyEnum.PAYMENT_CODE.getKey(), cmd.getKey());
    }

    public PaymentCodeDTO getPaymentCode() {
        String userId = UserContext.get();
        UserExtData data = userExtDomainService.get(userId, UserExtKeyEnum.PAYMENT_CODE.getKey());
        return MAPPER.toDTO(data);
    }

    @Transactional
    public void deletePaymentCode() {
        String userId = UserContext.get();
        UserExtData data = userExtDomainService.get(userId, UserExtKeyEnum.PAYMENT_CODE.getKey());
        if (data != null && data.getExtValue() != null) {
            qiniuClient.deleteFile(data.getExtValue());
        }
        userExtDomainService.delete(userId, UserExtKeyEnum.PAYMENT_CODE.getKey());
    }
}
