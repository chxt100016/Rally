package com.rally.tennis;

import com.rally.domain.tennis.TennisMatchQueryDomainService;
import com.rally.domain.tennis.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TennisTranslationService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TennisMatchAppService {

    @Resource
    private TennisMatchQueryDomainService tennisMatchQueryDomainService;

    @Resource
    private TennisTranslationService tennisTranslationService;

    @Cacheable(value = "upcoming", key = "#p0")
    public TennisMatchDTO upcoming(List<String> tournamentIds) {
        TennisMatchDTO dto = new TennisMatchDTO();
        dto.setSeed(tennisMatchQueryDomainService.seedGroups(tournamentIds));
        dto.setMatch(tennisMatchQueryDomainService.upcomingDateGroups(tournamentIds));
        translate(dto);
        return dto;
    }

    @Cacheable(value = "finished", key = "#p0")
    public TennisMatchDTO finished(List<String> tournamentIds) {
        TennisMatchDTO dto = new TennisMatchDTO();
        dto.setSeed(tennisMatchQueryDomainService.seedGroups(tournamentIds));
        dto.setMatch(tennisMatchQueryDomainService.finishedRoundGroups(tournamentIds));
        translate(dto);
        return dto;
    }

    private void translate(TennisMatchDTO dto) {
        List<MatchQueryVO> matches = new ArrayList<>();
        if (dto.getMatch() != null) {
            for (MatchGroupDTO group : dto.getMatch()) {
                collectMatches(group, matches);
            }
        }
        tennisTranslationService.matches(matches, TranslationLanguageEnum.ZH_CN);

        List<SeedVO> seeds = new ArrayList<>();
        if (dto.getSeed() != null) {
            for (SeedGroupDTO group : dto.getSeed()) {
                if (group.getData() != null) seeds.addAll(group.getData());
            }
        }
        tennisTranslationService.seeds(seeds, TranslationLanguageEnum.ZH_CN);
    }

    private void collectMatches(MatchGroupDTO group, List<MatchQueryVO> out) {
        if (group.getData() != null) out.addAll(group.getData());
        if (group.getChildren() != null) {
            for (MatchGroupDTO child : group.getChildren()) collectMatches(child, out);
        }
    }
}
