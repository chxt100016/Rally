package com.rally.domain.tournament.model;

import lombok.Data;

import java.util.List;

/**
 * 赛事参赛者汇总及按轮次分组数据。
 */
@Data
public class TournamentEntrantsDTO {
    /** 包含已退赛用户在内的总报名人数。 */
    private Integer totalCount;
    private Integer withdrawnCount;
    /** 已退赛用户不出现在轮次列表中。 */
    private List<TournamentEntrantRoundDTO> rounds;
}
