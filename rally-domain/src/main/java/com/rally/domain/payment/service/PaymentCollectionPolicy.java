package com.rally.domain.payment.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.utils.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发起收款的领域计算与校验（无副作用，见设计 §15.5）。
 */
@Service
public class PaymentCollectionPolicy {

    /**
     * 总额平摊：ceil 平均，余数算入第一人（D1）。纯计算。
     * 例：totalAmount=100, payerCount=3 → [34, 33, 33]
     */
    public int[] amortize(int totalAmount, int payerCount) {
        int base = totalAmount / payerCount;
        int remainder = totalAmount - base * payerCount;
        int[] result = new int[payerCount];
        for (int i = 0; i < payerCount; i++) {
            result[i] = base + (i < remainder ? 1 : 0);
        }
        return result;
    }

    /**
     * 发起收款参数校验：总额>0、应付人非空、每人金额可平摊（≥1 分）。
     */
    public void assertCollect(int totalAmount, List<String> payerUserIds) {
        Assert.isTrue(totalAmount > 0, BizErrorCode.PARAM_ERROR);
        Assert.isTrue(payerUserIds != null && !payerUserIds.isEmpty(), BizErrorCode.PARAM_ERROR);
        Assert.isTrue(payerUserIds.stream().allMatch(StringUtils::isNotBlank), BizErrorCode.PARAM_ERROR);
        Assert.isTrue(totalAmount >= payerUserIds.size(), BizErrorCode.PARAM_ERROR);
    }
}
