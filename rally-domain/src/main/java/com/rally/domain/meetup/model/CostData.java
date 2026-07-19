package com.rally.domain.meetup.model;

import lombok.Data;

import java.util.List;

/**
 * 费用数据（包含费用明细和按人时分摊数据）
 */
@Data
public class CostData {
    /** 费用明细列表 */
    private List<CostItem> costItems;
    /** 按人时分摊数据（可选） */
    private List<HourlyAllocation> hourlyAllocations;
}
