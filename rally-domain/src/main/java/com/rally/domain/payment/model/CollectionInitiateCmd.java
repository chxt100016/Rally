package com.rally.domain.payment.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 发起收款入参（活动结束后发起人触发）。金额单位「分」，后端计算每人金额，前端不可传每人金额。
 */
@Data
public class CollectionInitiateCmd {

    /** 约球ID */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 总额（分），系统按应付人数平摊 */
    @NotNull(message = "收款总额不能为空")
    @Min(value = 1, message = "收款总额必须大于0")
    private Integer totalAmount;

    /** 应付人 userId 列表；为空表示全体有效参与者（排除发起人自己） */
    private List<String> payerUserIds;
}
