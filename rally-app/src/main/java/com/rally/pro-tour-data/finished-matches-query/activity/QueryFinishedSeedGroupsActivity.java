package com.rally.protourdata.finishedmatchesquery.activity;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.model.SeedGroupDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 query-finished-seed-groups：组装已完赛查询的种子状态分组。
 */
@Component
@RequiredArgsConstructor
public class QueryFinishedSeedGroupsActivity {

    private final TourMatchQueryDomainService tourMatchQueryDomainService;

    public List<SeedGroupDTO> execute(List<String> tournamentIds) {
        // A1/A2/A3：复用 main 的类型化查询，保留签表/报名/球员/赛事关联、
        // FINISHED 败者映射、跨赛事 playerId 淘汰语义以及 ATP/WTA/OUT 分组与 seed 排序。
        return tourMatchQueryDomainService.seedGroups(tournamentIds);
    }
}
