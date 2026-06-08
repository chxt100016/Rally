package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.ActionStateEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 约球列表卡片视图（继承 DTO，增加计算字段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetupCardVO extends MeetupCardDTO {

    /** 每人费用（分） */
    private Integer perPersonCost;

    /** 距离（米） */
    private Double distanceMeters;

    /** 操作状态 */
    private ActionStateEnum actionState;

    // 发布者信息
    private String creatorNickname;
    private String creatorAvatarUrl;
}
