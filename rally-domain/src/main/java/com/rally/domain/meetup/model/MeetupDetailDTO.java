package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.ActionStateEnum;
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
    /** 操作状态 */
    private ActionStateEnum actionState;
}
