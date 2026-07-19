package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.MeetupRoleEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 参与者列表视图（含当前用户角色，前端据此区分创建人/参与者操作）
 */
@Data
@Accessors(chain = true)
public class ParticipantsViewDTO {
    /** 当前用户角色 */
    private MeetupRoleEnum userRole;
    /** 参与者列表 */
    private List<ParticipantDTO> list;
}
