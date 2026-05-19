package com.rally.tennis.parser;

import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.domain.tennis.model.ScheduledAtTextEnum;
import com.rally.tennis.convert.OopMatchAppConvertMapper;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.SetScore;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WtaOopMatchParser extends MatchParser<DrawParams, WtaMatchesResponse> {

    @Resource
    private WtaClient wtaClient;

    @Override
    public List<DrawResult<WtaMatchesResponse>> fetchDraws(DrawParams params) {
        WtaMatchesResponse response = wtaClient.getMatches(params.getTournamentId(), params.getYear());
        if (response == null || CollectionUtils.isEmpty(response.getMatches())) {
            return List.of();
        }
        return List.of(new DrawResult<>(response, Discipline.SINGLES, "LS",
                new DrawMeta(null, null), params.getTournamentId(), params.getYear()));
    }

    @Override
    public List<Match> getMatches(DrawResult<WtaMatchesResponse> draw, String tournamentId, Long drawId) {
        WtaMatchesResponse response = draw.getSlice();
        List<WtaMatchesResponse.MatchItem> filtered = response.getMatches().stream()
                .filter(m -> "M".equals(m.getDrawLevelType()) && "S".equals(m.getDrawMatchType()))
                .toList();

        List<Match> matches = new ArrayList<>();
        for (WtaMatchesResponse.MatchItem m : filtered) {
            Match match = new Match();
            match.setMatchId(m.getMatchID());
            match.setTournamentId(tournamentId);
            match.setYear(draw.getYear());
            match.setDrawId(drawId);
            match.setPlayer1Id(m.getPlayerIDA());
            match.setPlayer2Id(m.getPlayerIDB());
            match.setWinnerId(resolveWinner(m.getWinner(), m.getPlayerIDA(), m.getPlayerIDB()));
            match.setStatus(MatchStatus.toStatus(m.getMatchState()));
            match.setDurationMinutes(parseDuration(m.getMatchTimeTotal()));
            match.setCourt(m.getCourtName());
            match.setScheduledAt(OopMatchAppConvertMapper.INSTANCE.parseScheduledAt(
                    m.getMatchTimeStamp(), m.getNotBeforeISOTime()));
            match.setScheduledAtText(ScheduledAtTextEnum.fromText(m.getNotBeforeText()));
            if (m.getMatchTimeStamp() != null && !m.getMatchTimeStamp().isEmpty()) {
                try {
                    LocalDateTime ts = OffsetDateTime.parse(m.getMatchTimeStamp()).toLocalDateTime();
                    match.setStartedAt(ts);
                    if ("F".equals(m.getMatchState())) match.setEndedAt(ts);
                    match.setMatchDate(ts.toLocalDate());
                } catch (Exception ignored) {}
            }
            match.setSets(parseSets(m));
            matches.add(match);
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<WtaMatchesResponse> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<WtaMatchesResponse> draw, Long drawId) {
        return List.of();
    }

    private String resolveWinner(String winner, String playerA, String playerB) {
        if (winner == null || winner.isEmpty()) return null;
        try {
            int w = Integer.parseInt(winner);
            if (w == 0) return null;
            return w % 2 == 0 ? playerA : playerB;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseDuration(String matchTime) {
        if (matchTime == null || matchTime.isEmpty()) return null;
        try {
            String[] parts = matchTime.split(":");
            if (parts.length == 3) {
                return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim())
                        + (Integer.parseInt(parts[2].trim()) >= 30 ? 1 : 0);
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private List<SetScore> parseSets(WtaMatchesResponse.MatchItem m) {
        List<SetScore> sets = new ArrayList<>();
        String[][] setData = {
            {m.getScoreSet1A(), m.getScoreSet1B(), m.getScoreTbSet1()},
            {m.getScoreSet2A(), m.getScoreSet2B(), m.getScoreTbSet2()},
            {m.getScoreSet3A(), m.getScoreSet3B(), m.getScoreTbSet3()},
            {m.getScoreSet4A(), m.getScoreSet4B(), m.getScoreTbSet4()},
            {m.getScoreSet5A(), m.getScoreSet5B(), m.getScoreTbSet5()},
        };
        for (int i = 0; i < setData.length; i++) {
            String a = setData[i][0], b = setData[i][1], tb = setData[i][2];
            if (a == null || a.isEmpty()) break;
            SetScore s = new SetScore();
            s.setSetNumber(i + 1);
            try { s.setP1Games(Integer.parseInt(a)); } catch (NumberFormatException ignored) {}
            try { s.setP2Games(Integer.parseInt(b)); } catch (NumberFormatException ignored) {}
            if (tb != null && !tb.isEmpty()) {
                try { s.setP1Tiebreak(Integer.parseInt(tb)); } catch (NumberFormatException ignored) {}
            }
            sets.add(s);
        }
        return sets.isEmpty() ? null : sets;
    }

    @Override
    public CollectType collectType() {
        return CollectType.WTA_OOP;
    }
}
