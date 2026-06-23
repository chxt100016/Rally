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

    @Cacheable(value = "upcoming", key = "#tournamentIds")
    public TennisMatchDTO upcoming(List<String> tournamentIds) {
        TennisMatchDTO dto = new TennisMatchDTO();
        dto.setSeed(tennisMatchQueryDomainService.seedGroups(tournamentIds));
        dto.setMatch(tennisMatchQueryDomainService.upcomingCourtGroups(tournamentIds));
        translate(dto);
        return dto;
    }

    @Cacheable(value = "finished", key = "#tournamentIds")
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
                if (group.getData() != null) matches.addAll(group.getData());
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
}
