package com.rally.tennis.parser;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.SetScore;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AtpLiveMatchParser extends MatchParser<Void, List<MatchesResponse.MatchInfo>> {

    @Resource
    private TennisTvClient tennisTvClient;

    @Override
    public List<DrawResult<List<MatchesResponse.MatchInfo>>> fetchDraws(Void params) {
        MatchesResponse response = tennisTvClient.getMatchesByStatus("L");
        if (response == null || CollectionUtils.isEmpty(response.getMatches())) return List.of();

        Map<String, List<MatchesResponse.MatchInfo>> grouped = response.getMatches().stream()
                .filter(m -> m.getTournamentId() != null && m.getTournamentYear() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getTournamentId() + "|" + m.getTournamentYear()));

        List<DrawResult<List<MatchesResponse.MatchInfo>>> results = new ArrayList<>();
        for (List<MatchesResponse.MatchInfo> group : grouped.values()) {
            MatchesResponse.MatchInfo first = group.get(0);
            String tournamentId = String.valueOf(first.getTournamentId());
            int year = first.getTournamentYear();
            results.add(new DrawResult<>(group, Discipline.SINGLES, "MS",
                    new DrawMeta(null, null), tournamentId, year));
        }
        return results;
    }

    @Override
    public List<Match> getMatches(DrawResult<List<MatchesResponse.MatchInfo>> draw, String tournamentId, Long drawId) {
        List<Match> matches = new ArrayList<>();
        for (MatchesResponse.MatchInfo info : draw.getSlice()) {
            Match match = new Match();
            match.setMatchId(info.getMatchId());
            match.setTournamentId(tournamentId);
            match.setYear(info.getTournamentYear());
            match.setDrawId(drawId);
            match.setPlayer1Id(info.getPlayerTeam1() != null ? info.getPlayerTeam1().getPlayerId() : null);
            match.setPlayer2Id(info.getPlayerTeam2() != null ? info.getPlayerTeam2().getPlayerId() : null);
            match.setStatus(MatchStatus.toStatus(info.getStatus()));
            match.setCourt(info.getCourtName());
            match.setDurationMinutes(parseDuration(info.getMatchTime()));
            match.setCourtSeq(info.getCourtSeq());
            match.setSets(buildSets(info));
            matches.add(match);
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<List<MatchesResponse.MatchInfo>> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<List<MatchesResponse.MatchInfo>> draw, Long drawId) {
        return List.of();
    }

    @Override
    public boolean isLive() {
        return true;
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

    private List<SetScore> buildSets(MatchesResponse.MatchInfo info) {
        if (info.getPlayerTeam1() == null || info.getPlayerTeam2() == null) return null;
        List<MatchesResponse.SetInfo> sets1 = info.getPlayerTeam1().getSets();
        List<MatchesResponse.SetInfo> sets2 = info.getPlayerTeam2().getSets();
        if (CollectionUtils.isEmpty(sets1)) return null;

        List<SetScore> result = new ArrayList<>();
        for (MatchesResponse.SetInfo s1 : sets1) {
            SetScore ss = new SetScore();
            ss.setSetNumber(s1.getSetNumber());
            ss.setP1Games(s1.getSetScore() != null ? safeInt(s1.getSetScore()) : 0);
            ss.setP1Tiebreak(s1.getTieBreakScore() != null ? safeInt(s1.getTieBreakScore()) : null);
            if (CollectionUtils.isNotEmpty(sets2)) {
                sets2.stream()
                        .filter(s2 -> s2.getSetNumber() != null && s2.getSetNumber().equals(s1.getSetNumber()))
                        .findFirst()
                        .ifPresent(s2 -> {
                            ss.setP2Games(s2.getSetScore() != null ? safeInt(s2.getSetScore()) : 0);
                            ss.setP2Tiebreak(s2.getTieBreakScore() != null ? safeInt(s2.getTieBreakScore()) : null);
                        });
            }
            result.add(ss);
        }
        return result.isEmpty() ? null : result;
    }

    private Integer safeInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    @Override
    public CollectType collectType() {
        return CollectType.ATP_LIVE;
    }
}
