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
    /** 用户限制原因（NOT_REGISTERED 返回报名限制，FROZEN 返回手机号限制，可叠加） */
    private List<TournamentJoinRestrictionEnum> restrictions;
    private List<TournamentTimelineEventDTO> myTimeline;
    private TournamentBracketDTO bracket;
    private List<TournamentRejectRecordDTO> rejectRecords;
    /** 兼容原接口的扁平参赛者数组。 */
    private List<TournamentEntrantDTO> entrants;
    /** 参赛者统计及按轮次分组的新结构。 */
    private TournamentEntrantsDTO entrantOverview;
}
