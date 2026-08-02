package com.rally.domain.tournament.model;

import lombok.Data;

import java.util.List;

/**
 * 运营后台触发赛事匹配的参数。
 * manualGroups 非空时必须同时指定 tournamentId，每个内部数组是一场由运营指定的 entryNo 分组；
 * 指定分组先落地，剩余 WAITING 队伍继续走自动匹配。
 */
@Data
public class TournamentMatchRunCmd {
    private String tournamentId;
    private List<List<Integer>> manualGroups;
    /** 本次匹配临时排除的报名队伍编号；不改变其 WAITING 状态。 */
    private List<Integer> excludedEntryNos;
}
