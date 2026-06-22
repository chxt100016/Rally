package com.rally.tennis;

import com.rally.domain.tennis.MatchDataLoader;
import com.rally.domain.tennis.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TennisTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchQueryService {

    @Resource
    private MatchDataLoader matchDataLoader;

    @Resource
    private TennisTranslationService tennisTranslationService;

    public MatchQueryResponse queryMatches(String tournamentIdStr) {
        List<String> tournamentIds = MatchDataLoader.parseTournamentIds(tournamentIdStr);
        if (tournamentIds.isEmpty()) return emptyMatchResponse();

        MatchDataLoader.MatchLoadResult loaded = matchDataLoader.loadAndSplit(tournamentIds);
        if (loaded.empty) return emptyMatchResponse();

        Map<String, String> eliminatedRoundMap = new HashMap<>();
        for (MatchQueryVO m : loaded.finishedMatches) {
            if (m.getWinnerId() == null) continue;
            if (m.getPlayer1() != null && !m.getWinnerId().equals(m.getPlayer1().getId())) eliminatedRoundMap.put(m.getPlayer1().getId(), m.getRound());
            if (m.getPlayer2() != null && !m.getWinnerId().equals(m.getPlayer2().getId())) eliminatedRoundMap.put(m.getPlayer2().getId(), m.getRound());
        }

        List<SeedVO> seedVOList = loaded.seeds.stream()
                .filter(s -> s.getSeed() != null && s.getSeed() != 0)
                .map(s -> {
                    SeedVO seedVO = new SeedVO();
                    seedVO.setPlayerId(s.getPlayerId());
                    seedVO.setSeed(s.getSeed());
                    seedVO.setTournamentId(s.getTournamentId());
                    seedVO.setTour(loaded.tournamentTourMap.getOrDefault(s.getTournamentId(), ""));
                    PlayerData player = loaded.playerMap.get(s.getPlayerId());
                    if (player != null) {
                        seedVO.setName(StringUtils.isNotBlank(player.getLastName()) ? player.getLastName() : player.getFirstName());
                        seedVO.setCountry(CountryEnum.getCountry(player.getNationality()));
                    }
                    String eliminatedRound = eliminatedRoundMap.get(s.getPlayerId());
                    if (eliminatedRound != null) {
                        seedVO.setStatus(SeedStatusEnum.ELIMINATED);
                        seedVO.setLabel(TennisRoundEnum.labelOf(eliminatedRound));
                    } else {
                        seedVO.setStatus(SeedStatusEnum.ACTIVE);
                    }
                    return seedVO;
                })
                .sorted(Comparator.comparing(SeedVO::getSeed, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        loaded.upcomingMatches.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
        loaded.finishedMatches.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        tennisTranslationService.matches(loaded.upcomingMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.matches(loaded.finishedMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.seeds(seedVOList, TranslationLanguageEnum.ZH_CN);

        Map<String, List<MatchQueryVO>> matchMap = new LinkedHashMap<>();
        matchMap.put("upcomingMatches", loaded.upcomingMatches);
        matchMap.put("finishedMatches", loaded.finishedMatches);

        MatchQueryResponse response = new MatchQueryResponse();
        response.setSeeds(seedVOList);
        response.setMatches(matchMap);
        return response;
    }

    private MatchQueryResponse emptyMatchResponse() {
        MatchQueryResponse response = new MatchQueryResponse();
        response.setSeeds(List.of());
        Map<String, List<MatchQueryVO>> emptyMatch = new LinkedHashMap<>();
        emptyMatch.put("upcomingMatches", List.of());
        emptyMatch.put("finishedMatches", List.of());
        response.setMatches(emptyMatch);
        return response;
    }
}
