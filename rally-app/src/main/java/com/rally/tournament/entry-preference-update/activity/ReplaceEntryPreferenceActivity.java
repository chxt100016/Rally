package com.rally.tournament.entrypreferenceupdate.activity;

import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryUpdateCmd;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 replace-entry-preference：整组替换本人赛事报名的匹配偏好。 */
@Component
@RequiredArgsConstructor
public class ReplaceEntryPreferenceActivity {

    private final TournamentEntryService tournamentEntryService;

    /**
     * 按当前用户和赛事取得报名，校验非终态后原样替换三组偏好并保存。
     */
    @Transactional
    public void execute(TournamentEntryUpdateCmd command) {
        String userId = UserContext.get();

        // A1：沿用自然键查询；不存在时返回 TOURNAMENT_ENTRY_NOT_FOUND，不创建报名。
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(
                command.getTournamentId(), userId);

        /*
         * A2-A3：聚合拒绝 CHAMPION、ELIMINATED、WITHDRAWN；其余状态均可修改。
         * 服务将请求中的地区、订场能力和时间列表整组原样替换并普通保存，
         * 不清洗、去重或改动身份、状态、赛段、轮次和计数。
         */
        tournamentEntryService.updatePreference(entry, command);
    }
}
