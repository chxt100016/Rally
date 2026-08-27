package com.rally.protourdata.tournamentresultcollect.activity;

import com.rally.protourdata.tournamentlivecollect.activity.UpsertLiveMatchSnapshotsActivity;
import com.rally.protourdata.tournamentlivecollect.activity.UpsertTournamentDrawActivity;
import com.rally.tour.client.AtpCompletedMatchCollectClient;
import com.rally.tour.client.MatchCollectResult;
import com.rally.tour.parser.DrawParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 collect-completed-match-results：采集并保存 ATP/WTA 单打完赛快照。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectCompletedMatchResultsActivity {

    private final AtpCompletedMatchCollectClient completedMatchClient;
    private final UpsertTournamentDrawActivity upsertTournamentDrawActivity;
    private final UpsertLiveMatchSnapshotsActivity upsertMatchSnapshotsActivity;

    /**
     * 请求完赛来源，按目标巡回赛选择 MS/LS，再以两个独立事务保存签表和比赛。
     *
     * <p>保留 main 的现状：HTTP 入口构造的参数没有 tour，client 会先请求
     * 上游，随后在巡回赛枚举分流时失败，不会触发本地写入。</p>
     */
    public void execute(DrawParams target) {
        // A1-A2：completed client 保留原有 RPC、tour 枚举分流、事件身份
        // 校验、ATP=MS/WTA=LS 过滤及结果/状态/盘分转换。
        List<MatchCollectResult> sourceDraws = completedMatchClient.collect(target);

        for (MatchCollectResult sourceDraw : sourceDraws) {
            // A3：签表使用独立事务按 (tournamentId, year, MS/LS)
            // 关联或建立，完赛来源不刷新结构字段。
            Long drawId = upsertTournamentDrawActivity.execute(target, sourceDraw);
            if (drawId == null) {
                continue;
            }

            // A4：比赛在后续独立事务中按 drawId+matchId 合并。
            // 异常原样上抛，整个比赛批次回滚，不补偿已提交的签表。
            try {
                upsertMatchSnapshotsActivity.execute(drawId, sourceDraw.getMatches());
            } catch (RuntimeException exception) {
                log.error(
                        "完赛比赛保存失败: tournamentId={}, year={}, drawType={}, drawId={}, matchCount={}",
                        sourceDraw.getTournamentId(),
                        sourceDraw.getYear(),
                        sourceDraw.getDrawTypeCode(),
                        drawId,
                        sourceDraw.getMatches() == null ? 0 : sourceDraw.getMatches().size(),
                        exception);
                throw exception;
            }
        }
    }
}
