package com.rally.domain.tournament.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.utils.Assert;
import lombok.Getter;

/**
 * 报名聚合根：一个用户在一个赛事内的报名（stage/status 状态机）
 */
@Getter
public class TournamentEntry {

    private final TournamentEntryData data;

    public TournamentEntry(TournamentEntryData data) {
        this.data = data;
    }

    public String getEntryId() {
        return this.data.getBizId();
    }

    /** 创建报名：stage=QUALIFY、status=WAITING、currentRound=QUALIFIER */
    public static TournamentEntry create(String tournamentId, String userId, String partnerId, java.util.List<String> preferredDistricts, com.rally.domain.tournament.enums.CourtAbilityEnum courtAbility, java.util.List<String> availableTimes) {
        TournamentEntryData data = new TournamentEntryData();
        data.setBizId(IdWorker.getIdStr());
        data.setTournamentId(tournamentId);
        data.setUserId(userId);
        data.setPartnerId(partnerId);
        data.setPreferredDistricts(preferredDistricts);
        data.setCourtAbility(courtAbility);
        data.setAvailableTimes(availableTimes);
        data.setStage(TournamentEntryStageEnum.QUALIFY);
        data.setStatus(TournamentEntryStatusEnum.WAITING);
        data.setCurrentRound(TournamentRoundEnum.QUALIFIER);
        data.setQualifierRejectCount(0);
        data.setMainDrawRejectCount(0);
        return new TournamentEntry(data);
    }

    /** 仅排队阶段（未进入 IN_MATCH）可修改偏好 */
    public void assertCanUpdatePreference() {
        Assert.isTrue(this.data.getStatus() == TournamentEntryStatusEnum.WAITING, BizErrorCode.TOURNAMENT_ENTRY_STATUS_ILLEGAL);
    }

    public void updatePreference(java.util.List<String> preferredDistricts, com.rally.domain.tournament.enums.CourtAbilityEnum courtAbility, java.util.List<String> availableTimes) {
        assertCanUpdatePreference();
        this.data.setPreferredDistricts(preferredDistricts);
        this.data.setCourtAbility(courtAbility);
        this.data.setAvailableTimes(availableTimes);
    }

    /** 资格赛阶段是否可直接退出（WAITING/IN_MATCH 均可，无费用） */
    public boolean isQualifyStage() {
        return this.data.getStage() == TournamentEntryStageEnum.QUALIFY;
    }

    /** 正赛阶段（已支付）需先退款 */
    public boolean isMainStage() {
        return this.data.getStage() == TournamentEntryStageEnum.MAIN;
    }

    public void assertCanWithdraw() {
        Assert.isTrue(this.data.getStatus() != TournamentEntryStatusEnum.WITHDRAWN
                && this.data.getStatus() != TournamentEntryStatusEnum.ELIMINATED, BizErrorCode.TOURNAMENT_ENTRY_STATUS_ILLEGAL);
    }

    public void withdraw() {
        this.data.setStatus(TournamentEntryStatusEnum.WITHDRAWN);
    }

    /** 支付下单前置：必须处于资格赛待支付状态 */
    public void assertCanPay() {
        Assert.isTrue(this.data.getStatus() == TournamentEntryStatusEnum.PAYING, BizErrorCode.TOURNAMENT_ENTRY_STATUS_ILLEGAL);
    }

    /** 支付成功：PAYING→WAITING、stage QUALIFY→MAIN，写 paidTime，currentRound 置为正赛首轮 */
    public void advanceToMainDrawPaid(int totalSlots) {
        assertCanPay();
        this.data.setStatus(TournamentEntryStatusEnum.WAITING);
        this.data.setStage(TournamentEntryStageEnum.MAIN);
        this.data.setPaidTime(java.time.LocalDateTime.now());
        this.data.setCurrentRound(TournamentRoundEnum.firstMainRound(totalSlots));
    }
}
