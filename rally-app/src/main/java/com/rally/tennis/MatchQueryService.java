package com.rally.tennis;

import com.rally.domain.tennis.TennisMatchQueryDomainService;
import com.rally.domain.tennis.model.MatchQueryResponse;
import com.rally.domain.tennis.model.MatchQueryVO;
import com.rally.domain.tennis.model.SeedVO;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TennisTranslationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchQueryService {

    @Resource
    private TennisMatchQueryDomainService tennisMatchQueryDomainService;

    @Resource
    private TennisTranslationService tennisTranslationService;

    public MatchQueryResponse queryMatches(String tournamentIdStr) {
        List<String> tournamentIds = parseTournamentIds(tournamentIdStr);
        if (tournamentIds.isEmpty()) return emptyMatchResponse();

        List<SeedVO> seedVOList = tennisMatchQueryDomainService.seeds(tournamentIds);
        List<MatchQueryVO> upcomingMatches = tennisMatchQueryDomainService.upcomingMatches(tournamentIds);
        List<MatchQueryVO> finishedMatches = tennisMatchQueryDomainService.finishedMatches(tournamentIds);

        tennisTranslationService.matches(upcomingMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.matches(finishedMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.seeds(seedVOList, TranslationLanguageEnum.ZH_CN);

        Map<String, List<MatchQueryVO>> matchMap = new LinkedHashMap<>();
        matchMap.put("upcomingMatches", upcomingMatches);
        matchMap.put("finishedMatches", finishedMatches);

        MatchQueryResponse response = new MatchQueryResponse();
        response.setSeeds(seedVOList);
        response.setMatches(matchMap);
        return response;
    }

    private static List<String> parseTournamentIds(String tournamentIdStr) {
        if (tournamentIdStr == null || tournamentIdStr.isBlank()) return List.of();
        return Arrays.stream(tournamentIdStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
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
