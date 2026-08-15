package com.rally.tour;

import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.client.MatchCollectClient;
import com.rally.tour.client.MatchCollectResult;
import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawParams;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchCollectManager {

    @Value("${tour.collect.doubles:false}")
    private boolean collectDoubles;

    @Resource
    private TournamentCollectService tournamentCollectService;
    @Resource
    private DrawCollectService drawCollectService;
    @Resource
    private MatchCollectService matchCollectService;
    @Resource
    private PlayerCollectService playerCollectService;

    @Resource
    private List<MatchCollectClient> matchCollectClients;

    private Map<CollectType, MatchCollectClient> clients;

    @PostConstruct
    private void initClients() {
        clients = matchCollectClients.stream().collect(Collectors.toMap(
                        MatchCollectClient::collectType,
                        client -> client,
                        (a, b) -> {
                            throw new IllegalStateException("重复的比赛采集 Client: " + a.collectType());
                        },
                        () -> new EnumMap<>(CollectType.class)
                ));
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
        collect(params, client);
    }

    void collect(DrawParams params, MatchCollectClient client) {
        List<MatchCollectResult> draws = client.collect(params);

        for (MatchCollectResult draw : draws) {
            if (!shouldCollect(draw.getDiscipline())) continue;

            String tournamentId = draw.getTournamentId();
            if (!tournamentCollectService.exists(tournamentId)) {
                log.warn("Tournament not found, skip draw: {}", tournamentId);
                continue;
            }

            Long drawId = drawCollectService.saveOrUpdate(
                    tournamentId, draw.getYear(), draw.getDrawTypeCode(),
                    draw.getDrawMeta().getDrawSize(), draw.getDrawMeta().getTotalRounds());

            List<Match> matches = draw.getMatches();
            matches.forEach(match -> match.setDrawId(drawId));
            try {
                matchCollectService.saveMatches(matches);
            } catch (RuntimeException e) {
                log.error("比赛保存失败: collectType={}, tournamentId={}, year={}, drawType={}, drawId={}, matchCount={}",
                        client.collectType(), tournamentId, draw.getYear(), draw.getDrawTypeCode(), drawId,
                        matches.size(), e);
                throw e;
            }

            playerCollectService.savePlayers(draw.getPlayers());
            draw.getEntries().forEach(entry -> entry.setDrawId(drawId));
            tournamentCollectService.saveEntries(draw.getEntries());
        }
    }

    private boolean shouldCollect(Discipline discipline) {
        return discipline != Discipline.DOUBLES || collectDoubles;
    }
}
