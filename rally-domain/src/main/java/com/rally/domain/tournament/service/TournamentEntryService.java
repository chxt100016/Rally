package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
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

import java.util.List;

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
     * 双打（填了 partnerId）：若队友已报名则复用其编号并反向补齐队友的 partnerId，否则分配新编号；单打直接分配新编号
     */
    public TournamentEntry join(Tournament tournament, UserProfile userProfile, String userId, TournamentJoinCmd cmd) {
        tournamentPolicy.assertCanJoin(tournament, userProfile);

        TournamentEntryData existing = tournamentEntryRepository.findByTournamentAndUser(tournament.getTournamentId(), userId);
        Assert.isTrue(existing == null, BizErrorCode.TOURNAMENT_ALREADY_JOINED);

        int entryNo = resolveEntryNo(tournament.getTournamentId(), userId, cmd.getPartnerId());

        TournamentEntry entry = TournamentEntry.create(tournament.getTournamentId(), userId, cmd.getPartnerId(), entryNo, cmd.getPreferredDistricts(), cmd.getCourtAbility(), cmd.getAvailableTimes());
        tournamentEntryRepository.save(entry.getData());
        return entry;
    }

    /** 双打：若队友已报名，复用其编号并反向补齐队友的 partnerId；否则分配新编号（含单打） */
    private int resolveEntryNo(String tournamentId, String userId, String partnerId) {
        if (partnerId == null) {
            return tournamentEntryRepository.nextEntryNo(tournamentId);
        }
        TournamentEntryData partnerEntry = tournamentEntryRepository.findByTournamentAndUser(tournamentId, partnerId);
        if (partnerEntry == null) {
            return tournamentEntryRepository.nextEntryNo(tournamentId);
        }
        Assert.isTrue(partnerEntry.getPartnerId() == null || partnerEntry.getPartnerId().equals(userId), BizErrorCode.TOURNAMENT_PARTNER_ALREADY_PAIRED);
        if (partnerEntry.getPartnerId() == null) {
            partnerEntry.setPartnerId(userId);
            tournamentEntryRepository.save(partnerEntry);
        }
        return partnerEntry.getEntryNo();
    }

    /**
     * 修改报名偏好：仅排队阶段（未进入 IN_MATCH）允许
     */
    public void updatePreference(TournamentEntry entry, TournamentEntryUpdateCmd cmd) {
        entry.updatePreference(cmd.getPreferredDistricts(), cmd.getCourtAbility(), cmd.getAvailableTimes());
        tournamentEntryRepository.save(entry.getData());
    }

    /**
     * 退出赛事：置 WITHDRAWN（资格赛/正赛通用）。若本人正在比赛中，关比赛与对手回池由退赛活动处理
     */
    public void withdraw(TournamentEntry entry) {
        entry.assertCanWithdraw();
        entry.withdraw();
        tournamentEntryRepository.save(entry.getData());
    }

    /** 运营冻结指定用户的报名：仅允许 WAITING 状态。 */
    public void freeze(TournamentEntry entry) {
        entry.freeze();
        tournamentEntryRepository.save(entry.getData());
    }

    /** 解冻本人报名：必须已绑定手机号且当前状态为 FROZEN，恢复为 WAITING。 */
    public void unfreeze(Tournament tournament, TournamentEntry entry, UserProfile userProfile) {
        tournamentPolicy.assertCanUnfreeze(tournament);
        tournamentPolicy.assertPhoneBound(userProfile);
        entry.unfreeze();
        tournamentEntryRepository.save(entry.getData());
    }

    /**
     * 统计某轮次已晋级下一轮的人数（按 entryNo 去重）
     * 资格赛特殊：统计 currentRound 不等于 QUALIFIER 的人数（即已进入正赛的人）
     * 非资格赛：统计 currentRound 等于该轮下一轮的人数
     */
    public int countRoundEntry(String tournamentId, TournamentRoundEnum round) {
        List<TournamentEntryData> entries = tournamentEntryRepository.findByTournamentId(tournamentId);
        if (round == TournamentRoundEnum.QUALIFIER) {
            return (int) entries.stream()
                    .filter(e -> e.getCurrentRound() != TournamentRoundEnum.QUALIFIER)
                    .map(TournamentEntryData::getEntryNo)
                    .distinct()
                    .count();
        }
        TournamentRoundEnum nextRound = round.nextRound();
        if (nextRound == null) {
            return 0;
        }
        return (int) entries.stream()
                .filter(e -> e.getCurrentRound() == nextRound)
                .map(TournamentEntryData::getEntryNo)
                .distinct()
                .count();
    }
}
