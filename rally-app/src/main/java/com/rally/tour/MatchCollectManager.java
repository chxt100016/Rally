package com.rally.tour;

import com.rally.protourdata.tournamentdrawcollect.activity.UpsertDrawEntriesActivity;
import com.rally.protourdata.tournamentdrawcollect.activity.UpsertDrawMatchesActivity;
import com.rally.protourdata.tournamentdrawcollect.activity.UpsertDrawPlayersActivity;
import com.rally.protourdata.tournamentschedulecollect.activity.UpsertMatchSchedulesActivity;
import com.rally.protourdata.tournamentschedulecollect.activity.UpsertScheduleDrawActivity;
import com.rally.protourdata.tournamentschedulecollect.activity.UpsertScheduleEntriesActivity;
import com.rally.protourdata.tournamentschedulecollect.activity.UpsertSchedulePlayersActivity;
import com.rally.protourdata.tournamentlivecollect.activity.UpsertLiveMatchSnapshotsActivity;
import com.rally.tour.client.MatchCollectClient;
import com.rally.tour.client.MatchCollectResult;
import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchCollectManager {

    private final List<MatchCollectClient> matchCollectClients;
    private final com.rally.protourdata.tournamentdrawcollect.activity.UpsertTournamentDrawActivity
            upsertDrawActivity;
    private final UpsertDrawMatchesActivity upsertDrawMatchesActivity;
    private final UpsertDrawPlayersActivity upsertDrawPlayersActivity;
    private final UpsertDrawEntriesActivity upsertDrawEntriesActivity;
    private final UpsertScheduleDrawActivity upsertScheduleDrawActivity;
    private final UpsertMatchSchedulesActivity upsertMatchSchedulesActivity;
    private final UpsertSchedulePlayersActivity upsertSchedulePlayersActivity;
    private final UpsertScheduleEntriesActivity upsertScheduleEntriesActivity;
    private final com.rally.protourdata.tournamentlivecollect.activity.UpsertTournamentDrawActivity
            upsertLiveDrawActivity;
    private final UpsertLiveMatchSnapshotsActivity upsertLiveMatchSnapshotsActivity;

    private Map<CollectType, MatchCollectClient> clients;

    @PostConstruct
    private void initClients() {
        clients = matchCollectClients.stream().collect(Collectors.toMap(
                MatchCollectClient::collectType,
                client -> client,
                (first, second) -> {
                    throw new IllegalStateException("重复的比赛采集 Client: " + first.collectType());
                },
                () -> new EnumMap<>(CollectType.class)));
        EnumSet<CollectType> missing = EnumSet.allOf(CollectType.class);
        missing.removeAll(clients.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("缺少比赛采集 Client: " + missing);
        }
    }

    public void collect(CollectType type, DrawParams params) {
        MatchCollectClient client = clients.get(type);
        if (client == null) {
            throw new IllegalArgumentException("未注册比赛采集 Client: " + type);
        }
        List<MatchCollectResult> draws = client.collect(params);
        for (MatchCollectResult draw : draws) {
            switch (type.getPhase()) {
                case DRAW -> persistDraw(params, draw);
                case OOP -> persistSchedule(params, draw);
                case LIVE -> persistLive(params, draw);
            }
        }
    }

    private void persistDraw(DrawParams target, MatchCollectResult draw) {
        Long drawId = upsertDrawActivity.execute(draw);
        if (drawId == null) {
            return;
        }
        upsertDrawMatchesActivity.execute(drawId, draw.getMatches());
        upsertDrawPlayersActivity.execute(draw.getPlayers(), target.getTour());
        upsertDrawEntriesActivity.execute(drawId, draw.getEntries());
    }

    private void persistSchedule(DrawParams target, MatchCollectResult draw) {
        Long drawId = upsertScheduleDrawActivity.execute(target, draw);
        if (drawId == null) {
            return;
        }
        upsertMatchSchedulesActivity.execute(drawId, draw.getMatches());
        upsertSchedulePlayersActivity.execute(draw.getPlayers(), target.getTour());
        upsertScheduleEntriesActivity.execute(drawId, draw.getEntries());
    }

    private void persistLive(DrawParams target, MatchCollectResult draw) {
        Long drawId = upsertLiveDrawActivity.execute(target, draw);
        if (drawId != null) {
            upsertLiveMatchSnapshotsActivity.execute(drawId, draw.getMatches());
        }
    }
}
