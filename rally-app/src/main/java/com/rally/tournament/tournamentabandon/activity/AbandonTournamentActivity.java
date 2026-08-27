package com.rally.tournament.tournamentabandon.activity;

import com.rally.domain.tournament.model.TournamentAbandonCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 abandon-tournament：废弃一项尚未结束的赛事。 */
@Component
@RequiredArgsConstructor
public class AbandonTournamentActivity {

    private final TournamentAdminService tournamentAdminService;

    /**
     * 取得赛事、校验状态并在同一事务内标记为 ABANDONED。
     * 废弃原因当前不进入领域，也不联动任何赛事关联数据。
     */
    @Transactional
    public void execute(TournamentAbandonCmd cmd) {
        /*
         * A1-A3：领域服务按 bizId 取得聚合，聚合只允许 DRAFT/ACTIVE
         * 转为 ABANDONED，随后保存聚合。重复废弃继续返回状态错误；
         * 配置、进度、结束时间和所有关联对象均保持不变。
         */
        tournamentAdminService.abandon(cmd.getTournamentId());
    }
}
