package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentActionStateEnum;
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
    private List<TournamentTimelineEventDTO> myTimeline;
    private TournamentBracketDTO bracket;
    private List<TournamentRejectRecordDTO> rejectRecords;
}
