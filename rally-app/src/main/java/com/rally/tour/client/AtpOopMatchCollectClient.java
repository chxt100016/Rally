package com.rally.tour.client;

import com.rally.tour.parser.*;

import com.rally.client.tourtv.AtpTvClient;
import com.rally.client.tourtv.model.AtpOopResponse;
import com.rally.tour.convert.OopMatchAppConvertMapper;
import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.model.Player;
import com.rally.tour.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AtpOopMatchCollectClient extends AbstractMatchCollectClient<List<AtpOopResponse>, AtpOopResponse> {

    @Resource
    private AtpTvClient atpTvClient;

    /** 一次获取全部赛事 OOP 数据，每个赛事产生一个 MS DrawResult */
    @Override
    protected List<DrawResult<AtpOopResponse>> ms(List<AtpOopResponse> data, DrawParams params) {
        if (CollectionUtils.isEmpty(data)) return List.of();
        List<DrawResult<AtpOopResponse>> results = new ArrayList<>();
        for (AtpOopResponse tournament : data) {
            if (CollectionUtils.isEmpty(tournament.getOop())) continue;
            String tournamentId = String.valueOf(tournament.getId());
            Integer drawSize = tournament.getInfo() != null ? tournament.getInfo().getDrawSizeSM() : null;
            results.add(new DrawResult<>(tournament, Discipline.SINGLES, "MS",
                    new DrawMeta(drawSize, null), tournamentId, tournament.getYear()));
        }
        return results;
    }

    @Override
    protected List<AtpOopResponse> request(DrawParams params) {
        return atpTvClient.getOop(collectType().getApiUrl());
    }

    @Override
    public List<Match> getMatches(DrawResult<AtpOopResponse> draw, String tournamentId) {
        AtpOopResponse tournament = draw.getSlice();
        if (tournament == null || CollectionUtils.isEmpty(tournament.getOop())) return List.of();

        List<Match> matches = new ArrayList<>();
        for (AtpOopResponse.OopDay day : tournament.getOop()) {
            if (day.getCourts() == null) continue;
            for (AtpOopResponse.CourtDetail court : day.getCourts().values()) {
                if (CollectionUtils.isEmpty(court.getMatches())) continue;

                List<AtpOopResponse.MatchDetail> details = court.getMatches();
                LocalDateTime previousScheduledAt = null;
                for (AtpOopResponse.MatchDetail detail : details) {
                    LocalDateTime scheduledAt = OopMatchAppConvertMapper.INSTANCE.parseScheduledAt(
                            detail.getMatchDate(), detail.getNotBeforeISOTime());
                    if ("Followed By".equals(detail.getNotBeforeText()) && previousScheduledAt != null) {
                        // Followed By 的 ISO 时间可能沿用上一场时间，始终以同场上一场的结果递推。
                        scheduledAt = previousScheduledAt.plusMinutes(100);
                    }
                    if (scheduledAt != null) {
                        previousScheduledAt = scheduledAt;
                    }

                    // 先用完整球场赛程计算时间，再只输出 ATP 男单比赛。
                    if (!"ATP".equals(detail.getAssociationCode())
                            || detail.getMatchId() == null
                            || !detail.getMatchId().startsWith("MS")) continue;

                    Match match = OopMatchAppConvertMapper.INSTANCE.toMatch(detail);
                    match.setScheduledAt(scheduledAt);
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
    public List<TournamentEntry> getEntries(DrawResult<AtpOopResponse> draw) {
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.ATP_OOP;
    }
}
