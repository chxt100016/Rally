package com.rally.court.activity;

import com.rally.court.model.FetchedPois;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.gateway.CourtMapClient;
import com.rally.domain.court.model.CourtPoi;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务活动 fetch-amap-courts：逐个行政区划向地图服务分页检索网球场，汇总结果与失败区划。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FetchAmapCourtsActivity {

    private static final String KEYWORD = "网球场";
    private static final int PAGE_SIZE = 25;
    /** 相邻两次检索的间隔，避开地图服务的调用频率限制 */
    private static final long REQUEST_INTERVAL_MILLIS = 500L;

    private final CourtMapClient courtMapClient;

    public FetchedPois execute(List<String> regionCodes) {
        List<CourtPoi> all = new ArrayList<>();
        List<String> failedRegions = new ArrayList<>();
        for (String regionCode : regionCodes) {
            // A1 每个区划从第一页翻到取不到结果为止
            boolean failed = fetchRegion(regionCode, all);
            // A3 某个区划检索失败时跳过它继续下一个，把该区划编码记入失败清单
            if (failed) {
                failedRegions.add(regionCode);
            }
        }
        // A4 全部区划都失败且一条场所记录都没取到时报 COURT_COLLECT_FAILED；
        // 部分区划失败但仍有结果时按成功返回
        boolean allFailed = failedRegions.size() == regionCodes.size();
        Assert.isTrue(!(allFailed && all.isEmpty()), BizErrorCode.COURT_COLLECT_FAILED);

        FetchedPois result = new FetchedPois();
        result.setPois(all);
        result.setFetchedCount(all.size());
        result.setFailedRegions(failedRegions);
        return result;
    }

    /** @return true 表示该区划检索失败 */
    private boolean fetchRegion(String regionCode, List<CourtPoi> collector) {
        int pageNum = 1;
        while (true) {
            List<CourtPoi> page = courtMapClient.searchPage(KEYWORD, regionCode, pageNum, PAGE_SIZE);
            // A3 某一页失败即放弃该区划剩余页，已取到的前几页照常保留
            if (page == null) {
                return true;
            }
            // A1 翻页终止条件是某一页取不到任何场所记录
            if (page.isEmpty()) {
                return false;
            }
            collector.addAll(page);
            pageNum++;
            // A2 相邻两次检索之间留出固定间隔
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(REQUEST_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
