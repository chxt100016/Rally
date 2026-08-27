package com.rally.tournament.tournamentwithdraw.activity;

import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.service.TournamentEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 withdraw-tournament-entry：将本人有效赛事报名标记为已退赛。 */
@Component
@RequiredArgsConstructor
public class WithdrawTournamentEntryActivity {

    private final TournamentEntryService tournamentEntryService;

    /**
     * 按赛事和当前用户取得唯一报名，校验非终态后只将状态改为 WITHDRAWN。
     */
    @Transactional
    public void execute(String tournamentId, String userId) {
        // A1：报名不存在时沿用 TOURNAMENT_ENTRY_NOT_FOUND，不自动创建。
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(
                tournamentId, userId);

        /*
         * A2-A3：聚合拒绝 CHAMPION、WITHDRAWN、ELIMINATED 三种终态；
         * 其他非终态只更新 status，并以普通保存保留其余报名字段。
         */
        tournamentEntryService.withdraw(entry);
    }
}
