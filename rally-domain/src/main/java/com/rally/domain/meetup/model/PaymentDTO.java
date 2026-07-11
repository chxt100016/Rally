package com.rally.domain.meetup.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 支付信息 DTO
 */
@Data
@Accessors(chain = true)
public class PaymentDTO {
    /** 当前用户角色：COLLECTOR-收款人，PAYER-付款人 */
    private UserRoleEnum userRole;

    /** 费用明细列表 */
    private List<CostItem> costItems;

    /** 收款人的收款码 URL（仅收款人视角返回） */
    private String paymentCodeUrl;

    /** 每个人需要支付的金额（分） */
    private Integer amountPerPerson;

    /** 计算人数（用于显示，如"按4人计算"） */
    private Integer calculatedPlayerCount;

    public enum UserRoleEnum {
        /** 收款人 */
        COLLECTOR,
        /** 付款人 */
        PAYER,
        /** 陌生人（未加入活动） */
        STRANGER
    }
}
