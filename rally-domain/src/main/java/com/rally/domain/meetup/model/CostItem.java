package com.rally.domain.meetup.model;

import lombok.Data;

/**
 * 费用明细项
 */
@Data
public class CostItem {
    /** 费用名称 */
    private String name;
    /** 总金额（分） */
    private Integer totalAmount;
}
