package com.rally.user;

import com.rally.personalprofile.paymentcodedelete.activity.DeletePaymentCodeImageActivity;
import com.rally.personalprofile.paymentcodedelete.activity.RemovePaymentCodeRecordActivity;
import com.rally.personalprofile.paymentcodeget.activity.QueryPaymentCodeActivity;
import com.rally.personalprofile.paymentcodesave.activity.UpsertPaymentCodeRecordActivity;
import com.rally.user.model.PaymentCodeCmd;
import com.rally.user.model.PaymentCodeDTO;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCodeAppService {

    @Resource
    private UpsertPaymentCodeRecordActivity upsertPaymentCodeRecordActivity;

    @Resource
    private DeletePaymentCodeImageActivity deletePaymentCodeImageActivity;

    @Resource
    private RemovePaymentCodeRecordActivity removePaymentCodeRecordActivity;

    @Resource
    private QueryPaymentCodeActivity queryPaymentCodeActivity;

    @Transactional
    public void savePaymentCode(PaymentCodeCmd cmd) {
        String userId = UserContext.get();
        upsertPaymentCodeRecordActivity.execute(userId, cmd.getKey());
    }

    public PaymentCodeDTO getPaymentCode() {
        String userId = UserContext.get();
        return queryPaymentCodeActivity.execute(userId);
    }

    @Transactional
    public void deletePaymentCode() {
        String userId = UserContext.get();
        deletePaymentCodeImageActivity.execute(userId);
        removePaymentCodeRecordActivity.execute(userId);
    }
}
