package com.rally.tour;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.MatchGroupDTO;
import com.rally.domain.tour.model.MatchQueryVO;
import com.rally.domain.tour.model.PlayerVO;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TourContentAppServiceTest {

    @Test
    public void mergedAtpAndWtaTournamentUsesOneCourtSection() throws Exception {
        LocalDate today = LocalDate.now();
        TournamentData atp = tournament("atp-1", "ATP Event", "ATP", today);
        TournamentData wta = tournament("wta-1", "WTA Event", "WTA", today);
        TournamentGroupData tournamentGroup = new TournamentGroupData(atp, List.of(atp, wta));

        MatchGroupDTO court = new MatchGroupDTO();
        court.setKey("Centre Court");
        court.setName("Centre Court");
        court.setData(List.of(
                match("atp-1", "ATP One", "ATP Two", "10:00"),
                match("wta-1", "WTA One", "WTA Two", "Followed By")));

        MatchGroupDTO date = new MatchGroupDTO();
        date.setKey(today.toString());
        date.setChildren(List.of(court));

        TourContentAppService service = new TourContentAppService();
        inject(service, "tourTournamentQueryDomainService", new StubTournamentQueryService(tournamentGroup));
        inject(service, "tourMatchQueryDomainService", new StubMatchQueryService(List.of(date)));
        inject(service, "tourTranslationService", new NoOpTranslationService());

        String content = service.generateDailyContent();

        assertEquals(1, occurrences(content, "Centre Court\n"));
        assertEquals(1, occurrences(content, "ATP Event｜WTA Event赛程 | "));
        assertEquals(1, occurrences(content, "ATP One vs ATP Two"));
        assertEquals(1, occurrences(content, "WTA One vs WTA Two"));
    }

    private static TournamentData tournament(String id, String name, String tour, LocalDate date) {
        TournamentData tournament = new TournamentData();
        tournament.setTournamentId(id);
        tournament.setName(name);
        tournament.setTour(tour);
        tournament.setCity("Same City");
        tournament.setStartDate(date);
        tournament.setEndDate(date);
        return tournament;
    }

    private static MatchQueryVO match(String tournamentId, String player1Name, String player2Name, String scheduledShow) {
        MatchQueryVO match = new MatchQueryVO();
        match.setTournamentId(tournamentId);
        match.setCourt("Centre Court");
        match.setRoundShow("R16");
        match.setScheduledShow(scheduledShow);
        match.setPlayer1(player(player1Name));
        match.setPlayer2(player(player2Name));
        return match;
    }

    private static PlayerVO player(String name) {
        PlayerVO player = new PlayerVO();
        player.setName(name);
        return player;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = text.indexOf(value, fromIndex)) >= 0) {
            count++;
            fromIndex += value.length();
        }
        return count;
    }

    private static class StubTournamentQueryService extends TourTournamentQueryDomainService {
        private final TournamentGroupData group;

        private StubTournamentQueryService(TournamentGroupData group) {
            this.group = group;
        }

        @Override
        public List<TournamentGroupData> findValidCurrentTournamentGroups(LocalDate date) {
            return List.of(group);
        }
    }

    private static class StubMatchQueryService extends TourMatchQueryDomainService {
        private final List<MatchGroupDTO> dateGroups;

        private StubMatchQueryService(List<MatchGroupDTO> dateGroups) {
            this.dateGroups = dateGroups;
        }

        @Override
        public List<MatchGroupDTO> upcomingDateGroups(List<String> tournamentIds) {
            return dateGroups;
        }
    }

    private static class NoOpTranslationService extends TourTranslationService {
        @Override
        public Map<TranslationKey, String> translate(Set<TranslationKey> keys, TranslationLanguageEnum language) {
            return Map.of();
        }

        @Override
        public void matchGroups(List<MatchGroupDTO> groups, TranslationLanguageEnum language) {
        }

        @Override
        public void matches(List<MatchQueryVO> matches, TranslationLanguageEnum language) {
        }
    }
}
