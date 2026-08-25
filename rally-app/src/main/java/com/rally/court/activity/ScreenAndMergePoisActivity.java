package com.rally.court.activity;

import com.rally.domain.court.model.CourtPoi;
import com.rally.domain.court.model.CourtPoiScreenResult;
import com.rally.domain.court.service.CourtPoiScreenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 screen-and-merge-pois：筛掉非网球场记录，再把同一处场馆的多条记录并成一条。
 */
@Component
@RequiredArgsConstructor
public class ScreenAndMergePoisActivity {

    private final CourtPoiScreenerService courtPoiScreenerService;

    public CourtPoiScreenResult execute(List<CourtPoi> pois) {
        // A1 把全部场所记录交给筛选规则；A2 把保留下来的交给就近合并规则；
        // A3 每条主记录被并掉的记录名称汇成它的别名；A4 累加丢弃条数与被并掉的条数。
        // 四条动作由领域服务在一次调用里按规则完成，本活动只做编排与结论转交。
        return courtPoiScreenerService.screen(pois);
    }

    /** A3 主记录被并掉的记录名称，作为球场别名 */
    public List<String> aliasOf(com.rally.domain.court.model.CourtPoiCluster cluster) {
        return cluster.getMerged().stream().map(CourtPoi::getName).filter(java.util.Objects::nonNull).toList();
    }

    /** A4 丢弃条数 = 没通过校验的 + 通过校验但被并掉的 */
    public int filteredCountOf(CourtPoiScreenResult result) {
        return result.getRejectedCount() + result.getMergedCount();
    }
}
