package com.rally.tournament.entryfreeze.activity;

import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryFreezeCmd;
import com.rally.domain.tournament.service.TournamentEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 freeze-entry：将指定用户的待匹配报名冻结。 */
@Component
@RequiredArgsConstructor
public class FreezeEntryActivity {

    private final TournamentEntryService tournamentEntryService;

    /**
     * 按赛事与用户取得唯一报名，仅允许 WAITING 转为 FROZEN，并在同一事务内保存。
     */
    @Transactional
    public void execute(TournamentEntryFreezeCmd cmd) {
        // A1：不存在时沿用 TOURNAMENT_ENTRY_NOT_FOUND，不自动创建报名。
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(
                cmd.getTournamentId(), cmd.getUserId());

        /*
         * A2-A3：聚合只接受 WAITING；重复冻结及其他状态继续抛出
         * TOURNAMENT_ENTRY_STATUS_ILLEGAL。普通保存保持身份、赛段、轮次、
         * 偏好和其他字段不变，保存异常由事务回滚。
         */
        tournamentEntryService.freeze(entry);
    }
}
