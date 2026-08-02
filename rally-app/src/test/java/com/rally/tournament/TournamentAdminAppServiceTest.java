package com.rally.tournament;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotifySubscribeService;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.tournament.service.TournamentBatchMatchService;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TournamentAdminAppServiceTest {

    @Test
    public void batchMatchOrchestratesDomainCapabilityAndNotification() {
        TournamentData tournament = new TournamentData();
        tournament.setBizId("tournament-1");
        tournament.setTournamentName("周末网球挑战赛");

        MatchParticipantData participant = new MatchParticipantData();
        participant.setUserId("user-1");
        TournamentMatch match = new TournamentMatch(new TournamentMatchData(), List.of(participant));

        RecordingBatchMatchService batchMatchService = new RecordingBatchMatchService(tournament, match);
        RecordingNotifySubscribeService notifySubscribeService = new RecordingNotifySubscribeService();
        TournamentAdminAppService appService = new TournamentAdminAppService(
                null, null, batchMatchService, notifySubscribeService);

        appService.runTournamentMatch();

        assertEquals(1, batchMatchService.qualifierRunCount);
        assertEquals(1, batchMatchService.mainRoundRunCount);
        assertEquals(1, notifySubscribeService.notifyCount);
        assertEquals(List.of("user-1"), notifySubscribeService.userIds);
        assertEquals("匹配成功", notifySubscribeService.data.get("phrase2"));
    }

    private static class RecordingBatchMatchService extends TournamentBatchMatchService {

        private final TournamentData tournament;
        private final TournamentMatch qualifierMatch;
        private int qualifierRunCount;
        private int mainRoundRunCount;

        private RecordingBatchMatchService(TournamentData tournament, TournamentMatch qualifierMatch) {
            super(null, null, null, null);
            this.tournament = tournament;
            this.qualifierMatch = qualifierMatch;
        }

        @Override
        public List<TournamentData> listTournamentsToMatch(LocalDateTime matchTime) {
            return List.of(tournament);
        }

        @Override
        public List<TournamentMatch> matchQualifier(String tournamentId) {
            qualifierRunCount++;
            return List.of(qualifierMatch);
        }

        @Override
        public List<TournamentMatch> matchMainRoundsAll(String tournamentId) {
            mainRoundRunCount++;
            return List.of();
        }
    }

    private static class RecordingNotifySubscribeService extends NotifySubscribeService {

        private int notifyCount;
        private List<String> userIds;
        private Map<String, Object> data;

        private RecordingNotifySubscribeService() {
            super(null, List.of());
        }

        @Override
        public void notify(NotifyBizType bizType, String refBizId, NoticeScene scene,
                           List<String> userIds, Map<String, Object> data) {
            notifyCount++;
            this.userIds = userIds;
            this.data = data;
        }
    }
}
