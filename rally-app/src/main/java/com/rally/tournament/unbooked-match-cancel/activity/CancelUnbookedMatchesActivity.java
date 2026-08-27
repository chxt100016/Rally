package com.rally.tournament.unbookedmatchcancel.activity;

import com.rally.domain.tournament.service.TournamentBatchMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 cancel-unbooked-matches：批量删除未提交赛约的比赛并释放报名。
 */
@Component
@RequiredArgsConstructor
public class CancelUnbookedMatchesActivity {

    private final TournamentBatchMatchService tournamentBatchMatchService;

    /**
     * 整批共用同一事务；任一单场复核、条件删除或报名保存失败均回滚整批。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId) {
        /*
         * A1-A4：既有批量服务保留赛事存在与 currentRound 校验，
         * 初筛 MATCHED/BOOKING 后逐场重载，按状态条件物理删除比赛及参与关系，
         * 并且只将尚处于 IN_MATCH 的报名退回 WAITING。无候选与非
         * IN_MATCH 报名均幂等跳过；条件删除未命中作为并发冲突向外抛出。
         * 此活动不处理赛约、不发通知、不触发重新匹配。
         */
        tournamentBatchMatchService.cancelUnsubmittedMatches(tournamentId);
    }
}
