package com.rally.tournament.entryunfreeze.activity;

import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryUnfreezeCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 unfreeze-entry：将本人冻结报名恢复到等待匹配。 */
@Component
@RequiredArgsConstructor
public class UnfreezeEntryActivity {

    private final TournamentAdminService tournamentAdminService;
    private final UserProfileDomainService userProfileDomainService;
    private final TournamentEntryService tournamentEntryService;

    /**
     * 校验赛事、本人手机及冻结报名后，在同一事务内恢复 WAITING。
     */
    @Transactional
    public void execute(TournamentEntryUnfreezeCmd cmd) {
        String userId = UserContext.get();

        // A1：事赛必须存在、处于 ACTIVE 且未超过结束时间。
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());

        // A2：用户必须存在，且本人手机已绑定。
        UserProfile userProfile = userProfileDomainService.get(userId);

        // A3：只按赛事和当前用户取本人唯一报名，不自动创建。
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(
                cmd.getTournamentId(), userId);

        /*
         * A1-A4：领域服务依次校验赛事解冻资格、手机绑定与 FROZEN
         * 精确来源状态；仅将状态改为 WAITING 后保存，其他报名字段保持不变。
         */
        tournamentEntryService.unfreeze(tournament, entry, userProfile);
    }
}
