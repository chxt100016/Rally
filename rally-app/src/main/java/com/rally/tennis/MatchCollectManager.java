package com.rally.tennis;

import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.parser.CollectType;
import com.rally.tennis.parser.DrawParams;
import com.rally.tennis.parser.DrawResult;
import com.rally.tennis.parser.MatchParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
        parsers = matchParsers.stream().collect(Collectors.toMap(
                        MatchParser::collectType,
                        p -> p,
                        (a, b) -> a,
                        () -> new EnumMap<>(CollectType.class)
                ));
    }

    @SuppressWarnings("unchecked")
    public void collect(CollectType type, DrawParams params) {
        MatchParser<Object, Object> parser = (MatchParser<Object, Object>) parsers.get(type);
        collect(params, parser);
    }

    public <R, S> void collect(DrawParams params, MatchParser<R, S> parser) {
        List<DrawResult<S>> draws = parser.fetch(params);

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

            List<Match> matches = parser.getMatches(draw, tournamentId, drawId);
            matchCollectService.saveMatches(matches);

            List<Player> players = parser.getPlayers(draw);
            // 统一在 manager 层设置 tour，parser 无需感知
            players.forEach(p -> p.setTour(params.getTour()));
            playerCollectService.savePlayers(players);
            tournamentCollectService.saveEntries(parser.getEntries(draw, drawId));
        }
    }

    private boolean shouldCollect(Discipline discipline) {
        return discipline != Discipline.DOUBLES || collectDoubles;
    }
}
