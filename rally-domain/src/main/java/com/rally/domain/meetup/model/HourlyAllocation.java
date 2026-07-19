package com.rally.domain.meetup.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 按人时分摊数据
 */
@Data
public class HourlyAllocation {
    /** 时长（小时），可以是小数（如 0.5 表示半小时，1.0 表示 1 小时）*/
    private BigDecimal duration;
    /** 参与的用户 ID 列表 */
    private List<String> userIds;
}
