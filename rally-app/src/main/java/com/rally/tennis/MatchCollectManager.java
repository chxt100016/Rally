package com.rally.tennis;

import com.rally.tennis.model.Discipline;
import com.rally.tennis.parser.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchCollectManager {

    @Value("${tennis.collect.doubles:false}")
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
    private List<MatchParser<?, ?>> matchParsers;

    private Map<CollectType, MatchParser<?, ?>> parsers;

    @PostConstruct
    private void initParsers() {
        parsers = matchParsers.stream()
                .collect(Collectors.toMap(
                        MatchParser::collectType,
                        p -> p,
                        (a, b) -> a,
                        () -> new EnumMap<>(CollectType.class)
                ));
    }

    @SuppressWarnings("unchecked")
    public <P> void collect(CollectType type, P params) {
        MatchParser<P, Object> parser = (MatchParser<P, Object>) parsers.get(type);
        collectFromDraw(params, parser);
    }

    public <P, S> void collectFromDraw(P params, MatchParser<P, S> parser) {
        List<DrawResult<S>> draws = parser.fetchDraws(params);
        if (CollectionUtils.isEmpty(draws)) return;

        for (DrawResult<S> draw : draws) {
            if (!shouldCollect(draw.getDiscipline())) continue;

            String tournamentId = draw.getTournamentId();
            if (!tournamentCollectService.exists(tournamentId)) {
                log.warn("Tournament not found, skip draw: {}", tournamentId);
                continue;
            }

            Long drawId = drawCollectService.saveOrUpdate(
                    tournamentId, draw.getYear(), draw.getDrawTypeCode(),
                    draw.getMeta().getDrawSize(), draw.getMeta().getTotalRounds());

            List<com.rally.tennis.model.Match> matches = parser.getMatches(draw, tournamentId, drawId);
            if (parser.isLive()) {
                matchCollectService.updateMatches(matches);
            } else {
                matchCollectService.saveMatches(matches);
            }

            playerCollectService.savePlayers(parser.getPlayers(draw));
            tournamentCollectService.saveEntries(parser.getEntries(draw, drawId));
        }
    }

    private boolean shouldCollect(Discipline discipline) {
        return discipline != Discipline.DOUBLES || collectDoubles;
    }
}
