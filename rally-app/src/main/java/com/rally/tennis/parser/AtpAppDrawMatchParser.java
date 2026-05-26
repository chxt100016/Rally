package com.rally.tennis.parser;

import com.rally.client.atp.AtpClient;
import com.rally.client.atp.model.AtpAppDrawResponse;
import com.rally.domain.tennis.model.TennisRoundEnum;
import com.rally.tennis.convert.AtpAppDrawMatchConvertMapper;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AtpAppDrawMatchParser extends MatchParser<AtpAppDrawResponse, AtpAppDrawResponse> {

    @Resource
    private AtpClient atpClient;

    @Override
    protected AtpAppDrawResponse fetchData(DrawParams params) {
        return atpClient.getDraws(params.getTournamentId(), params.getYear());
    }

    @Override
    protected List<DrawResult<AtpAppDrawResponse>> fetchMs(AtpAppDrawResponse data, DrawParams params) {
        if (data == null || data.getData() == null) return List.of();
        AtpAppDrawResponse.Data d = data.getData();
        if (CollectionUtils.isEmpty(d.getResults())) return List.of();
        int drawSize = d.getEvent() != null && d.getEvent().getSglDrawSize() != null
                ? d.getEvent().getSglDrawSize() : 0;
        int rounds = d.getResults().size();
        return List.of(new DrawResult<>(data, Discipline.SINGLES, "MS",
                new DrawMeta(drawSize, rounds),
                params.getTournamentId(), params.getYear()));
    }

    @Override
    public List<Match> getMatches(DrawResult<AtpAppDrawResponse> draw, String tournamentId, Long drawId) {
        AtpAppDrawResponse data = draw.getSlice();
        if (data == null || data.getData() == null || CollectionUtils.isEmpty(data.getData().getResults())) {
            return List.of();
        }
        List<Match> matches = new ArrayList<>();
        for (AtpAppDrawResponse.RoundResult roundResult : data.getData().getResults()) {
            if (CollectionUtils.isEmpty(roundResult.getMatches())) continue;
            AtpAppDrawResponse.Round round = roundResult.getRound();
            for (AtpAppDrawResponse.Match m : roundResult.getMatches()) {
                Match match = AtpAppDrawMatchConvertMapper.INSTANCE.toMatch(m);
                match.setTournamentId(tournamentId);
                match.setDrawId(drawId);
                match.setYear(draw.getYear());
                if (round != null) {
                    match.setRoundNumber(round.getId() != null ? Integer.parseInt(round.getId()) : null);
                    match.setRoundName(TennisRoundEnum.toShortName(round.getLongName()));
                }
                matches.add(match);
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<AtpAppDrawResponse> draw) {
        AtpAppDrawResponse data = draw.getSlice();
        if (data == null || data.getData() == null || CollectionUtils.isEmpty(data.getData().getResults())) {
            return List.of();
        }
        Map<String, Player> playerMap = new HashMap<>();
        for (AtpAppDrawResponse.RoundResult roundResult : data.getData().getResults()) {
            if (CollectionUtils.isEmpty(roundResult.getMatches())) continue;
            for (AtpAppDrawResponse.Match m : roundResult.getMatches()) {
                if (m.getPlayerId() != null) {
                    playerMap.computeIfAbsent(m.getPlayerId(), id -> {
                        Player p = new Player();
                        p.setPlayerId(id);
                        p.setFirstName(m.getPlayerFirstName());
                        p.setLastName(m.getPlayerLastName());
                        p.setTour("ATP");
                        return p;
                    });
                }
                if (m.getOpponentId() != null) {
                    playerMap.computeIfAbsent(m.getOpponentId(), id -> {
                        Player p = new Player();
                        p.setPlayerId(id);
                        p.setFirstName(m.getOpponentFirstName());
                        p.setLastName(m.getOpponentLastName());
                        p.setTour("ATP");
                        return p;
                    });
                }
            }
        }
        return new ArrayList<>(playerMap.values());
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<AtpAppDrawResponse> draw, Long drawId) {
        AtpAppDrawResponse data = draw.getSlice();
        if (data == null || data.getData() == null) return List.of();

        // 从 SeededPlayers 建立 playerId -> seed 映射
        Map<String, Integer> seedMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(data.getData().getSeededPlayers())) {
            for (AtpAppDrawResponse.SeededPlayer sp : data.getData().getSeededPlayers()) {
                if (sp.getPlayerId() != null && sp.getSeed() != null) {
                    seedMap.put(sp.getPlayerId(), sp.getSeed());
                }
            }
        }

        // 从 Results 收集所有球员及其 entryType，合并 seed
        Map<String, TournamentEntry> entryMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(data.getData().getResults())) {
            for (AtpAppDrawResponse.RoundResult roundResult : data.getData().getResults()) {
                if (CollectionUtils.isEmpty(roundResult.getMatches())) continue;
                for (AtpAppDrawResponse.Match m : roundResult.getMatches()) {
                    if (m.getPlayerId() != null) {
                        entryMap.computeIfAbsent(m.getPlayerId(), id -> {
                            TournamentEntry entry = new TournamentEntry();
                            entry.setPlayerId(id);
                            entry.setDrawId(drawId);
                            Integer seed = seedMap.get(id);
                            entry.setSeed(seed != null ? seed.shortValue() : null);
                            entry.setEntryType(m.getPlayerEntryType());
                            return entry;
                        });
                    }
                    if (m.getOpponentId() != null) {
                        entryMap.computeIfAbsent(m.getOpponentId(), id -> {
                            TournamentEntry entry = new TournamentEntry();
                            entry.setPlayerId(id);
                            entry.setDrawId(drawId);
                            Integer seed = seedMap.get(id);
                            entry.setSeed(seed != null ? seed.shortValue() : null);
                            entry.setEntryType(m.getOpponentEntryType());
                            return entry;
                        });
                    }
                }
            }
        }
        return new ArrayList<>(entryMap.values());
    }

    @Override
    public CollectType collectType() {
        return CollectType.ATP_APP_DRAW;
    }
}
