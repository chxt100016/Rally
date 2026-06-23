package com.rally.tour;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TourMatchAppService {

    @Resource
    private TourMatchQueryDomainService tourMatchQueryDomainService;

    @Resource
    private TourTranslationService tourTranslationService;

    @Cacheable(value = "upcoming", key = "#p0")
    public TourMatchDTO upcoming(List<String> tournamentIds) {
        TourMatchDTO dto = new TourMatchDTO();
        dto.setSeed(tourMatchQueryDomainService.seedGroups(tournamentIds));
        dto.setMatch(tourMatchQueryDomainService.upcomingDateGroups(tournamentIds));
        translate(dto);
        return dto;
    }

    @Cacheable(value = "finished", key = "#p0")
    public TourMatchDTO finished(List<String> tournamentIds) {
        TourMatchDTO dto = new TourMatchDTO();
        dto.setSeed(tourMatchQueryDomainService.seedGroups(tournamentIds));
        dto.setMatch(tourMatchQueryDomainService.finishedRoundGroups(tournamentIds));
        translate(dto);
        return dto;
    }

    private void translate(TourMatchDTO dto) {
        if (dto.getMatch() != null) {
            tourTranslationService.matchGroups(dto.getMatch(), TranslationLanguageEnum.ZH_CN);

            List<MatchQueryVO> matches = new ArrayList<>();
            for (MatchGroupDTO group : dto.getMatch()) {
                collectMatches(group, matches);
            }
            tourTranslationService.matches(matches, TranslationLanguageEnum.ZH_CN);
        }

        List<SeedVO> seeds = new ArrayList<>();
        if (dto.getSeed() != null) {
            for (SeedGroupDTO group : dto.getSeed()) {
                if (group.getData() != null) seeds.addAll(group.getData());
            }
        }
        tourTranslationService.seeds(seeds, TranslationLanguageEnum.ZH_CN);
    }

    private void collectMatches(MatchGroupDTO group, List<MatchQueryVO> out) {
        if (group.getData() != null) out.addAll(group.getData());
        if (group.getChildren() != null) {
            for (MatchGroupDTO child : group.getChildren()) collectMatches(child, out);
        }
    }
}
