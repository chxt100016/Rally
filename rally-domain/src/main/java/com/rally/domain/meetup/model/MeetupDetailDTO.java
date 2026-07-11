package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.ActionStateEnum;
import com.rally.domain.meetup.enums.JoinRestrictionEnum;
import com.rally.domain.recap.model.RecapDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 约球详情 DTO（四部分 + 操作状态）
 */
@Data
@Accessors(chain = true)
public class MeetupDetailDTO {
    /** 比赛基本信息 */
    private MeetupDTO meetup;
    /** 创建人信息 */
    private CreatorDTO creator;
    /** 参与人列表（创建人视角包含待审批） */
    private List<ParticipantDTO> participants;
    /** 赛后收集（活动结束时返回） */
    private RecapDTO recap;
    /** 天气信息 */
    private WeatherDTO weather;
    /** 操作状态 */
    private ActionStateEnum actionState;
    /** 是否可报名（仅 actionState 为 JOIN_DIRECT/APPLY_APPROVAL 时返回，其余为 null） */
    private Boolean joinable;
    /** 不可报名的限制原因（可叠加；joinable=false 时非空，文案由前端拼装） */
    private List<JoinRestrictionEnum> restrictions;
    /** im未读消息 */
    private Integer unreadCount;
    /** 支付视图（有费用明细时返回；由 app 层详情编排组装） */
    private PaymentDTO payment;

}
