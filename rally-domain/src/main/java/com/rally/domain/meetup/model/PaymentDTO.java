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

    /** 总价（分，由 costItems 汇总计算） */
    private Integer totalAmount;

    /** 分摊模式：AVERAGE-人均分摊，HOURLY-按人时分摊 */
    private AllocationModeEnum allocationMode;

    /** 当前用户需要支付的金额（分） */
    private Integer currentUserAmount;

    /** 计算人数（用于显示，如"按4人计算"） */
    private Integer calculatedPlayerCount;

    /** 按人时分摊数据（用于回显） */
    private List<HourlyAllocation> hourlyAllocations;

    /** 当前用户的人时分摊详情文案（仅设置了按人时分摊时返回，如"4人2小时、3人1小时"） */
    private String allocationDesc;

    public enum UserRoleEnum {
        /** 收款人 */
        COLLECTOR,
        /** 付款人 */
        PAYER,
        /** 陌生人（未加入活动） */
        STRANGER
    }

    public enum AllocationModeEnum {
        /** 人均分摊 */
        AVERAGE,
        /** 按人时分摊 */
        HOURLY
    }
}
