package com.rally.tournament.tournamententry.activity;

import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentJoinCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 register-tournament-entry：校验参赛资格并创建资格赛等待报名。 */
@Component
@RequiredArgsConstructor
public class RegisterTournamentEntryActivity {

    private final TournamentAdminService tournamentAdminService;
    private final TournamentEntryService tournamentEntryService;
    private final UserProfileDomainService userProfileDomainService;

    /**
     * 保持既有报名链路的校验顺序、搭档编号分配和错误语义。
     */
    @Transactional
    public TournamentEntry execute(TournamentJoinCmd command, String userId) {
        // A1-A2：赛事与用户均按既有服务加载，再校验基础资料和网球档案完整度。
        Tournament tournament = tournamentAdminService.get(command.getTournamentId());
        UserProfile userProfile = userProfileDomainService.get(userId);
        userProfile.assertCompleted();

        /*
         * A2-A5：领域服务继续按 main 的顺序校验 ACTIVE、报名窗口、手机、
         * 性别和精确 NTRP，拒绝任意状态旧报名。随后按可选搭档报名复用或
         * 分配 entryNo，必要时补齐搭档反向 partnerId，并保存初始化为
         * QUALIFY/WAITING/QUALIFIER、两类拒绝计数为零的新报名。
         * 不增加比赛类型、搭档资格、资格赛容量或偏好元素清洗等额外校验。
         */
        return tournamentEntryService.join(tournament, userProfile, userId, command);
    }
}
