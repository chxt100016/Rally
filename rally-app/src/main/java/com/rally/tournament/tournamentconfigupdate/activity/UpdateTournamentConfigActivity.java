package com.rally.tournament.tournamentconfigupdate.activity;

import com.rally.domain.tournament.model.TournamentUpdateCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 update-tournament-config：完整更新赛事配置并保留运营进度。
 */
@Component
@RequiredArgsConstructor
public class UpdateTournamentConfigActivity {

    private final TournamentAdminService tournamentAdminService;

    /**
     * 保持既有更新链路的任意状态支持、弱校验、非空列策略和事务异常语义。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(TournamentUpdateCmd command) {
        /*
         * A1-A4：既有领域服务按业务编号加载赛事，执行创建级配置校验，
         * 仅映射创建命令声明的配置字段并保存。offlineFromRound 仍可显式
         * 清空，其余空值由仓储实体更新策略忽略；状态、轮次、锁位、冠军、
         * 结束时间及线下活动绑定均不会被映射覆盖。
         */
        tournamentAdminService.update(command);
    }
}
