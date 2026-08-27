package com.rally.tournament.tournamentdraftcreate.activity;

import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentCreateCmd;
import com.rally.domain.tournament.model.TournamentIdDTO;
import com.rally.domain.tournament.service.TournamentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 create-tournament-draft：校验完整配置并创建赛事草稿。 */
@Component
@RequiredArgsConstructor
public class CreateTournamentDraftActivity {

    private final TournamentAdminService tournamentAdminService;

    /**
     * 保持既有创建链路的字段校验、城市解析、初始值与异常语义。
     */
    @Transactional(rollbackFor = Exception.class)
    public TournamentIdDTO execute(TournamentCreateCmd command) {
        /*
         * A1-A5：领域创建命令依次执行赛制、金额和时间校验，按 cityCode
         * 解析 cityName，生成 bizId，装配全部配置和 DRAFT/QUALIFIER/0
         * 初始进度后保存。城市缺失、枚举组装或持久化异常继续由统一异常
         * 处理收敛为 OPERATION_FAILED；成功时只交付赛事业务编号。
         */
        Tournament tournament = tournamentAdminService.create(command);
        return new TournamentIdDTO(tournament.getTournamentId());
    }
}
