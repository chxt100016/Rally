package com.rally.tennis.parser;

import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaDrawsResponse;
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

@Component
public class WtaDrawMatchParser extends MatchParser<WtaDrawsResponse, WtaDrawsResponse.DrawData> {

    @Resource
    private WtaClient wtaClient;

    @Override
    protected WtaDrawsResponse fetchData(DrawParams params) {
        return wtaClient.getDraws(params.getTournamentId(), params.getYear());
    }

    @Override
    protected List<DrawResult<WtaDrawsResponse.DrawData>> fetchLs(WtaDrawsResponse data, DrawParams params) {
        if (data == null || data.getData() == null
                || CollectionUtils.isEmpty(data.getData().getResults())) return List.of();
        WtaDrawsResponse.DrawData drawData = data.getData();
        Integer drawSize = drawData.getEvent() != null ? drawData.getEvent().getSglDrawSize() : null;
        Integer totalRounds = drawData.getResults().size();
        return List.of(new DrawResult<>(drawData, Discipline.SINGLES, "LS",
                new DrawMeta(drawSize, totalRounds), params.getTournamentId(), params.getYear()));
    }

    @Override
    public List<Match> getMatches(DrawResult<WtaDrawsResponse.DrawData> draw, String tournamentId, Long drawId) {
        WtaDrawsResponse.DrawData data = draw.getSlice();
        if (data == null || CollectionUtils.isEmpty(data.getResults())) return List.of();

        List<Match> matches = new ArrayList<>();
        for (WtaDrawsResponse.RoundResult roundResult : data.getResults()) {
            if (roundResult.getRound() == null || CollectionUtils.isEmpty(roundResult.getMatches())) continue;
            WtaDrawsResponse.RoundInfo round = roundResult.getRound();
            Integer roundNumber = parseRoundId(round.getId());
            String roundName = round.getShortName();

            for (WtaDrawsResponse.MatchResult m : roundResult.getMatches()) {
                Match match = new Match();
                match.setMatchId(m.getMatchId());
                match.setTournamentId(tournamentId);
                match.setDrawId(drawId);
                match.setYear(draw.getYear());
                match.setRoundNumber(roundNumber);
                match.setRoundName(roundName);
                match.setPlayer1Id("0".equals(m.getPlayerId()) ? null : m.getPlayerId());
                match.setPlayer2Id("0".equals(m.getOpponentId()) ? null : m.getOpponentId());
                match.setWinnerId("0".equals(m.getWinningPlayerId()) ? null : m.getWinningPlayerId());
                match.setStatus(MatchStatus.toStatus(m.getMState()));
                match.setSets(parseDrawSets(m));
                matches.add(match);
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<WtaDrawsResponse.DrawData> draw) {
        WtaDrawsResponse.DrawData data = draw.getSlice();
        if (data == null || CollectionUtils.isEmpty(data.getDraw())) return List.of();
        List<Player> players = new ArrayList<>();
        for (WtaDrawsResponse.DrawEntry entry : data.getDraw()) {
            if ("0".equals(entry.getPlayerId())) continue;
            Player player = new Player();
            player.setPlayerId(entry.getPlayerId());
            player.setFirstName(entry.getPlayerFirstName());
            player.setLastName(entry.getPlayerLastName());
            player.setNationality(entry.getPlayerNatlId());
            player.setTour("WTA");
            players.add(player);
        }
        return players;
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<WtaDrawsResponse.DrawData> draw, Long drawId) {
        WtaDrawsResponse.DrawData data = draw.getSlice();
        if (data == null || CollectionUtils.isEmpty(data.getDraw())) return List.of();
        List<TournamentEntry> entries = new ArrayList<>();
        for (WtaDrawsResponse.DrawEntry e : data.getDraw()) {
            if ("0".equals(e.getPlayerId())) continue;
            TournamentEntry entry = new TournamentEntry();
            entry.setPlayerId(e.getPlayerId());
            entry.setDrawId(drawId);
            entry.setSeed(e.getSeed() != null ? e.getSeed().shortValue() : null);
            entry.setEntryType(e.getEntryType());
            entries.add(entry);
        }
        return entries;
    }

    private Integer parseRoundId(String id) {
        if (id == null) return null;
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<SetScore> parseDrawSets(WtaDrawsResponse.MatchResult m) {
        List<SetScore> sets = new ArrayList<>();
        Integer[][] setData = {
            {m.getSet1Player(), m.getSet1Opponent(), m.getSet1Tie()},
            {m.getSet2Player(), m.getSet2Opponent(), m.getSet2Tie()},
            {m.getSet3Player(), m.getSet3Opponent(), m.getSet3Tie()},
            {m.getSet4Player(), m.getSet4Opponent(), m.getSet4Tie()},
            {m.getSet5Player(), m.getSet5Opponent(), m.getSet5Tie()},
        };
        for (int i = 0; i < setData.length; i++) {
            if (setData[i][0] == null) break;
            SetScore s = new SetScore();
            s.setSetNumber(i + 1);
            s.setP1Games(setData[i][0]);
            s.setP2Games(setData[i][1]);
            s.setP1Tiebreak(setData[i][2]);
            sets.add(s);
        }
        return sets.isEmpty() ? null : sets;
    }

    @Override
    public CollectType collectType() {
        return CollectType.WTA_DRAW;
    }
}
