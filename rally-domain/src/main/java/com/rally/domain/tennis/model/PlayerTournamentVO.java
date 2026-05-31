package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

/**
 * 球员弹窗响应 VO
 */
@Data
public class PlayerTournamentVO {

    private PlayerTournamentDetailVO player;
    /** 晋级路线（已完成的比赛，按轮次升序） */
    private List<MatchProgressVO> progressPath;
    /** 前方对手（后续每轮预测对手） */
    private List<MatchProgressVO> upcomingOpponents;
    /** 出局信息，仅已淘汰球员不为 null */
    private MatchProgressVO eliminationInfo;
}
