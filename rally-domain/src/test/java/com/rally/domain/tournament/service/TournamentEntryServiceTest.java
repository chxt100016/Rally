package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class TournamentEntryServiceTest {

    @Test
    public void shouldUnfreezeAndSaveEntryWhenPhoneExists() {
        RecordingTournamentEntryRepository repository = new RecordingTournamentEntryRepository();
        TournamentEntryService service = new TournamentEntryService(repository, new TournamentPolicy());
        TournamentEntryData data = frozenEntryData();

        service.unfreeze(activeTournament(), new TournamentEntry(data), userProfile("13800000000"));

        assertEquals(TournamentEntryStatusEnum.WAITING, data.getStatus());
        assertSame(data, repository.saved);
    }

    @Test
    public void shouldNotUnfreezeOrSaveEntryWhenPhoneIsMissing() {
        RecordingTournamentEntryRepository repository = new RecordingTournamentEntryRepository();
        TournamentEntryService service = new TournamentEntryService(repository, new TournamentPolicy());
        TournamentEntryData data = frozenEntryData();

        try {
            service.unfreeze(activeTournament(), new TournamentEntry(data), userProfile(null));
            fail("未绑定手机号时应拒绝解冻");
        } catch (BusinessException e) {
            assertEquals(BizErrorCode.USER_PHONE_REQUIRED, e.getErrorCode());
        }
        assertEquals(TournamentEntryStatusEnum.FROZEN, data.getStatus());
        assertNull(repository.saved);
    }

    @Test
    public void shouldNotUnfreezeAfterTournamentEnded() {
        RecordingTournamentEntryRepository repository = new RecordingTournamentEntryRepository();
        TournamentEntryService service = new TournamentEntryService(repository, new TournamentPolicy());
        TournamentEntryData data = frozenEntryData();
        TournamentData tournamentData = activeTournament().getData();
        tournamentData.setEndTime(LocalDateTime.now().minusMinutes(1));

        try {
            service.unfreeze(new Tournament(tournamentData), new TournamentEntry(data), userProfile("13800000000"));
            fail("赛事结束后应拒绝解冻");
        } catch (BusinessException e) {
            assertEquals(BizErrorCode.TOURNAMENT_STATUS_ILLEGAL, e.getErrorCode());
        }
        assertEquals(TournamentEntryStatusEnum.FROZEN, data.getStatus());
        assertNull(repository.saved);
    }

    private Tournament activeTournament() {
        TournamentData data = new TournamentData();
        data.setStatus(TournamentStatusEnum.ACTIVE);
        return new Tournament(data);
    }

    private TournamentEntryData frozenEntryData() {
        TournamentEntryData data = new TournamentEntryData();
        data.setStatus(TournamentEntryStatusEnum.FROZEN);
        return data;
    }

    private UserProfile userProfile(String phone) {
        UserData user = new UserData();
        user.setPhone(phone);
        return UserProfile.create(user, null);
    }

    private static class RecordingTournamentEntryRepository implements TournamentEntryRepository {
        private TournamentEntryData saved;

        @Override
        public void save(TournamentEntryData data) {
            this.saved = data;
        }

        @Override
        public TournamentEntryData findByBizId(String bizId) {
            return null;
        }

        @Override
        public TournamentEntryData findByTournamentAndUser(String tournamentId, String userId) {
            return null;
        }

        @Override
        public List<TournamentEntryData> findByTournamentId(String tournamentId) {
            return List.of();
        }

        @Override
        public List<TournamentEntryData> findWaitingByTournamentAndStage(String tournamentId, TournamentEntryStageEnum stage, TournamentRoundEnum round) {
            return List.of();
        }

        @Override
        public List<TournamentRoundEnum> findDistinctWaitingRounds(String tournamentId, TournamentEntryStageEnum stage) {
            return List.of();
        }

        @Override
        public int nextEntryNo(String tournamentId) {
            return 1;
        }
    }
}
