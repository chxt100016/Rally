package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentEntryUpdateCmd;
import com.rally.domain.tournament.model.TournamentJoinCmd;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 报名领域服务（用户端：报名/修改偏好/退出）
 */
@Service
@RequiredArgsConstructor
public class TournamentEntryService {

    private final TournamentEntryRepository tournamentEntryRepository;

    private final TournamentPolicy tournamentPolicy;

    /**
     * 获取报名聚合根
     */
    public TournamentEntry get(String entryId) {
        TournamentEntryData data = tournamentEntryRepository.findByBizId(entryId);
        Assert.notNull(data, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(data);
    }

    /**
     * 获取当前用户在某赛事的报名聚合根
     */
    public TournamentEntry getByTournamentAndUser(String tournamentId, String userId) {
        TournamentEntryData data = tournamentEntryRepository.findByTournamentAndUser(tournamentId, userId);
        Assert.notNull(data, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(data);
    }

    /**
     * 报名：校验赛事 ACTIVE、在报名开放窗口内、性别限制符合、未重复报名；创建 Entry（WAITING）
     */
    public TournamentEntry join(Tournament tournament, UserProfile userProfile, String userId, TournamentJoinCmd cmd) {
        tournamentPolicy.assertCanJoin(tournament, userProfile);

        TournamentEntryData existing = tournamentEntryRepository.findByTournamentAndUser(tournament.getTournamentId(), userId);
        Assert.isTrue(existing == null, BizErrorCode.TOURNAMENT_ALREADY_JOINED);

        TournamentEntry entry = TournamentEntry.create(tournament.getTournamentId(), userId, cmd.getPartnerId(), cmd.getPreferredDistricts(), cmd.getCourtAbility(), cmd.getAvailableTimes());
        tournamentEntryRepository.save(entry.getData());
        return entry;
    }

    /**
     * 修改报名偏好：仅排队阶段（未进入 IN_MATCH）允许
     */
    public void updatePreference(TournamentEntry entry, TournamentEntryUpdateCmd cmd) {
        entry.updatePreference(cmd.getPreferredDistricts(), cmd.getCourtAbility(), cmd.getAvailableTimes());
        tournamentEntryRepository.save(entry.getData());
    }

    /**
     * 资格赛阶段直接退出：置 WITHDRAWN
     */
    public void withdrawQualify(TournamentEntry entry) {
        entry.assertCanWithdraw();
        entry.withdraw();
        tournamentEntryRepository.save(entry.getData());
    }
}
