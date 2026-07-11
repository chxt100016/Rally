package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 修改约球价格入参
 */
@Data
public class MeetupEditPriceCmd {

    /** 约球ID，必传 */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 费用明细 */
    private List<CostItem> costItems;
}
