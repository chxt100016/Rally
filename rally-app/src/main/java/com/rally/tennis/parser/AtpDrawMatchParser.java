package com.rally.tennis.parser;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.domain.tennis.model.TennisRoundEnum;
import com.rally.tennis.convert.DrawMatchAppConvertMapper;
import com.rally.tennis.convert.PlayerAppConvertMapper;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AtpDrawMatchParser extends MatchParser<AtpDrawsResponse, AtpDrawsResponse.Draw> {

    @Resource
    private TennisTvClient tennisTvClient;

    @Override
    protected AtpDrawsResponse fetchData(DrawParams params) {
        return tennisTvClient.getDraws(params.getTournamentId(), params.getYear());
    }

    @Override
    protected List<DrawResult<AtpDrawsResponse.Draw>> fetchMs(AtpDrawsResponse data, DrawParams params) {
        if (data == null) return List.of();
        AtpDrawsResponse.Draw ms = data.getMS();
        if (ms == null || CollectionUtils.isEmpty(ms.getRounds())) return List.of();
        return List.of(new DrawResult<>(ms, Discipline.SINGLES, "MS",
                new DrawMeta(ms.getDrawSize(), ms.getRounds().size()),
                params.getTournamentId(), params.getYear()));
    }

//    @Override
//    protected List<DrawResult<AtpDrawsResponse.Draw>> fetchMd(AtpDrawsResponse data, DrawParams params) {
//        if (data == null) return List.of();
//        AtpDrawsResponse.Draw md = data.getMD();
//        if (md == null || CollectionUtils.isEmpty(md.getRounds())) return List.of();
//        return List.of(new DrawResult<>(md, Discipline.DOUBLES, "MD",
//                new DrawMeta(md.getDrawSize(), md.getRounds().size()),
//                params.getTournamentId(), params.getYear()));
//    }

    @Override
    public List<Match> getMatches(DrawResult<AtpDrawsResponse.Draw> draw, String tournamentId, Long drawId) {
        AtpDrawsResponse.Draw drawData = draw.getSlice();
        if (drawData == null || CollectionUtils.isEmpty(drawData.getRounds())) {
            return List.of();
        }
        List<Match> matches = new ArrayList<>();
        for (AtpDrawsResponse.Round round : drawData.getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) continue;
            for (AtpDrawsResponse.Fixture fixture : round.getFixtures()) {
                Match match = DrawMatchAppConvertMapper.INSTANCE.toMatch(fixture);
                match.setTournamentId(tournamentId);
                match.setDrawId(drawId);
                match.setYear(draw.getYear());
                match.setRoundNumber(round.getRoundId());
                match.setRoundName(TennisRoundEnum.of(round.getRoundName()));
                matches.add(match);
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<AtpDrawsResponse.Draw> draw) {
        AtpDrawsResponse.Draw drawData = draw.getSlice();
        if (drawData == null || CollectionUtils.isEmpty(drawData.getRounds())) return List.of();
        List<Player> players = new ArrayList<>();
        for (AtpDrawsResponse.Round round : drawData.getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) continue;
            for (AtpDrawsResponse.Fixture fixture : round.getFixtures()) {
                if (fixture.getResult() == null) continue;
                if (fixture.getResult().getTeamTop() != null
                        && fixture.getResult().getTeamTop().getPlayer() != null) {
                    players.add(PlayerAppConvertMapper.INSTANCE
                            .toPlayerFromDraw(fixture.getResult().getTeamTop().getPlayer()));
                }
                if (fixture.getResult().getTeamBottom() != null
                        && fixture.getResult().getTeamBottom().getPlayer() != null) {
                    players.add(PlayerAppConvertMapper.INSTANCE
                            .toPlayerFromDraw(fixture.getResult().getTeamBottom().getPlayer()));
                }
            }
        }
        players.forEach(p -> p.setTour("ATP"));
        return players;
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<AtpDrawsResponse.Draw> draw, Long drawId) {
        AtpDrawsResponse.Draw drawData = draw.getSlice();
        if (drawData == null || CollectionUtils.isEmpty(drawData.getRounds())) return List.of();
        Map<String, TournamentEntry> entryMap = new LinkedHashMap<>();
        for (AtpDrawsResponse.Round round : drawData.getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) continue;
            for (AtpDrawsResponse.Fixture fixture : round.getFixtures()) {
                extractFromDrawLine(fixture.getDrawLineTop(), drawId, entryMap);
                extractFromDrawLine(fixture.getDrawLineBottom(), drawId, entryMap);
            }
        }
        return new ArrayList<>(entryMap.values());
    }

    private void extractFromDrawLine(AtpDrawsResponse.DrawLine drawLine, Long drawId,
                                     Map<String, TournamentEntry> entryMap) {
        if (drawLine == null || CollectionUtils.isEmpty(drawLine.getPlayers())) return;
        for (AtpDrawsResponse.PlayerInfo playerInfo : drawLine.getPlayers()) {
            if (playerInfo == null || playerInfo.getPlayerId() == null) continue;
            TournamentEntry entry = new TournamentEntry();
            entry.setPlayerId(playerInfo.getPlayerId());
            entry.setDrawId(drawId);
            entry.setSeed(drawLine.getSeed() != null ? drawLine.getSeed().shortValue() : null);
            entryMap.put(playerInfo.getPlayerId(), entry);
        }
    }

    @Override
    public CollectType collectType() {
        return CollectType.ATP_DRAW;
    }
}
