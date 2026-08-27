package com.rally.tournament.tournamentactivate.activity;

import com.rally.domain.tournament.model.TournamentActivateCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 activate-tournament：开放一项配置好核心时间的赛事草稿。 */
@Component
@RequiredArgsConstructor
public class ActivateTournamentActivity {

    private final TournamentAdminService tournamentAdminService;

    /**
     * 取得赛事、校验草稿状态与核心时间，并在同一事务内激活。
     */
    @Transactional
    public void execute(TournamentActivateCmd cmd) {
        /*
         * A1-A3：领域服务按 bizId 取得聚合，聚合仅接受 DRAFT，且只校验
         * registrationStartTime 严格早于 qualifierStartTime；随后保存为 ACTIVE。
         * 其他配置、轮次、席位与关联数据均不改变，重复调用继续返回状态错误。
         */
        tournamentAdminService.activate(cmd.getTournamentId());
    }
}
