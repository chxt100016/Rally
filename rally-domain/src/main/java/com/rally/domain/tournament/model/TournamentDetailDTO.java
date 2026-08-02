package com.rally.domain.tournament.model;

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
    /** 线下赛活动；未创建时为 null。 */
    private TournamentOfflineDTO offline;
    private TournamentEntryDTO myEntry;
    private MyCurrentMatchDTO myCurrentMatch;
    /** 当前用户的赛事待办状态及展示文案。 */
    private TournamentActionDTO action;
    /** 是否可报名（未报名时返回；NOT_REGISTERED_CLOSED 固定为 false） */
    private Boolean joinable;
    /** 用户准入限制原因（仅 actionState 为 NOT_REGISTERED 时返回，可叠加） */
    private List<TournamentJoinRestrictionEnum> restrictions;
    private List<TournamentTimelineEventDTO> myTimeline;
    private TournamentBracketDTO bracket;
    private List<TournamentRejectRecordDTO> rejectRecords;
    private List<TournamentEntrantDTO> entrants;
}
