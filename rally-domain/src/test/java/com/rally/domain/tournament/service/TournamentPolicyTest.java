package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentCreateCmd;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserProfile;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TournamentPolicyTest {

    private final TournamentPolicy tournamentPolicy = new TournamentPolicy();

    @Test
    public void shouldAllowJoinWhenNtrpLevelsAreNumericallyEqual() {
        tournamentPolicy.assertCanJoin(tournament("3.5"), userProfile("3.50"));
    }

    @Test
    public void shouldRejectJoinWhenNtrpLevelsDoNotMatch() {
        assertNtrpLevelNotMatch(userProfile("3.0"));
    }

    @Test
    public void shouldRejectJoinWhenUserNtrpLevelIsMissing() {
        assertNtrpLevelNotMatch(UserProfile.create(null, null));
    }

    @Test
    public void shouldAllowPowerOfTwoTotalSlotsFromTwoToSixtyFour() {
        for (int totalSlots : new int[]{2, 4, 8, 16, 32, 64}) {
            tournamentPolicy.assertParam(createCmd(totalSlots));
        }
        assertEquals(TournamentRoundEnum.FINAL, TournamentRoundEnum.firstMainRound(2));
        assertEquals(TournamentRoundEnum.ROUND_4, TournamentRoundEnum.firstMainRound(4));
        assertEquals(TournamentRoundEnum.ROUND_8, TournamentRoundEnum.firstMainRound(8));
    }

    private void assertNtrpLevelNotMatch(UserProfile userProfile) {
        try {
            tournamentPolicy.assertCanJoin(tournament("3.5"), userProfile);
            fail("NTRP等级不匹配时应拒绝报名");
        } catch (BusinessException e) {
            assertEquals(BizErrorCode.TOURNAMENT_NTRP_LEVEL_NOT_MATCH, e.getErrorCode());
        }
    }

    private Tournament tournament(String ntrpLevel) {
        TournamentData data = new TournamentData();
        data.setNtrpLevel(ntrpLevel);
        data.setStatus(TournamentStatusEnum.ACTIVE);
        data.setGenderLimit(TournamentGenderLimitEnum.ALL);
        data.setRegistrationStartTime(LocalDateTime.now().minusMinutes(1));
        return new Tournament(data);
    }

    private UserProfile userProfile(String ntrpLevel) {
        TennisProfileData profile = new TennisProfileData();
        profile.setNtrpScore(new BigDecimal(ntrpLevel));
        return UserProfile.create(null, profile);
    }

    private TournamentCreateCmd createCmd(int totalSlots) {
        LocalDateTime now = LocalDateTime.now();
        TournamentCreateCmd cmd = new TournamentCreateCmd();
        cmd.setTotalSlots(totalSlots);
        cmd.setOfflineFromRound(TournamentRoundEnum.QUALIFIER);
        cmd.setEntryFee(0L);
        cmd.setRegistrationStartTime(now);
        cmd.setQualifierStartTime(now.plusMinutes(1));
        return cmd;
    }
}
