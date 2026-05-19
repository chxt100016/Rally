package com.rally.tennis.parser;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.AtpOopResponse;
import com.rally.tennis.convert.OopMatchAppConvertMapper;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AtpOopMatchParser extends MatchParser<List<AtpOopResponse>, AtpOopResponse> {

    @Resource
    private TennisTvClient tennisTvClient;

    /** 一次获取全部赛事 OOP 数据，每个赛事产生一个 MS DrawResult */
    @Override
    protected List<DrawResult<AtpOopResponse>> fetchMs(List<AtpOopResponse> data, DrawParams params) {
        if (CollectionUtils.isEmpty(data)) return List.of();
        List<DrawResult<AtpOopResponse>> results = new ArrayList<>();
        for (AtpOopResponse tournament : data) {
            if (CollectionUtils.isEmpty(tournament.getOop())) continue;
            // 预过滤：只保留 matchId 以 "MS" 开头的比赛
            for (AtpOopResponse.OopDay day : tournament.getOop()) {
                if (day.getCourts() == null) continue;
                for (AtpOopResponse.CourtDetail court : day.getCourts().values()) {
                    if (CollectionUtils.isEmpty(court.getMatches())) continue;
                    court.setMatches(court.getMatches().stream()
                            .filter(m -> m.getMatchId() != null && m.getMatchId().startsWith("MS"))
                            .collect(Collectors.toList()));
                }
            }
            String tournamentId = String.valueOf(tournament.getId());
            Integer drawSize = tournament.getInfo() != null ? tournament.getInfo().getDrawSizeSM() : null;
            results.add(new DrawResult<>(tournament, Discipline.SINGLES, "MS",
                    new DrawMeta(drawSize, null), tournamentId, tournament.getYear()));
        }
        return results;
    }

    @Override
    protected List<AtpOopResponse> fetchData(DrawParams params) {
        return tennisTvClient.getOop();
    }

    @Override
    public List<Match> getMatches(DrawResult<AtpOopResponse> draw, String tournamentId, Long drawId) {
        AtpOopResponse tournament = draw.getSlice();
        if (tournament == null || CollectionUtils.isEmpty(tournament.getOop())) return List.of();

        List<Match> matches = new ArrayList<>();
        for (AtpOopResponse.OopDay day : tournament.getOop()) {
            if (day.getCourts() == null) continue;
            for (AtpOopResponse.CourtDetail court : day.getCourts().values()) {
                if (CollectionUtils.isEmpty(court.getMatches())) continue;

                LocalDateTime lastMatchScheduledAt = null;
                for (AtpOopResponse.MatchDetail detail : court.getMatches()) {
                    if (!"ATP".equals(detail.getAssociationCode())) continue;

                    Match match = OopMatchAppConvertMapper.INSTANCE.toMatch(detail);
                    match.setDrawId(drawId);

                    if ("Followed By".equals(detail.getNotBeforeText()) && match.getScheduledAt() == null) {
                        if (lastMatchScheduledAt != null) {
                            match.setScheduledAt(lastMatchScheduledAt.plusMinutes(70));
                        }
                    }
                    if (match.getScheduledAt() != null) {
                        lastMatchScheduledAt = match.getScheduledAt();
                    }
                    matches.add(match);
                }
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<AtpOopResponse> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<AtpOopResponse> draw, Long drawId) {
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.ATP_OOP;
    }
}
