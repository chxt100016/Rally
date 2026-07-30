package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import com.rally.domain.tournament.enums.TournamentJoinRestrictionEnum;
import lombok.Data;

import java.util.List;

/**
 * 落地页详情聚合返回
 */
@Data
public class TournamentDetailDTO {
    private TournamentDTO tournament;
    private TournamentProgressDTO progress;
    private TournamentEntryDTO myEntry;
    private MyCurrentMatchDTO myCurrentMatch;
    private TournamentActionStateEnum actionState;
    /** 当前用户状态卡片文案，由 actionState 驱动。 */
    private TournamentActionStateTextDTO actionStateText;
    /** 是否可报名（仅 actionState 为 NOT_REGISTERED 时返回，其余为 null） */
    private Boolean joinable;
    /** 不可报名的限制原因（可叠加；joinable=false 时非空，文案由前端拼装） */
    private List<TournamentJoinRestrictionEnum> restrictions;
    private List<TournamentTimelineEventDTO> myTimeline;
    private TournamentBracketDTO bracket;
    private List<TournamentRejectRecordDTO> rejectRecords;
    private List<TournamentEntrantDTO> entrants;
}
