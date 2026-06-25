package com.rally.tour;

import com.rally.domain.tour.repository.MatchQueryRepository;
import com.rally.domain.tour.repository.TourDrawRepository;
import com.rally.domain.tour.repository.TourEntryRepository;
import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.TourDrawData;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentEntryData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourContentAppService {


    public String generateDailyContent(LocalDate date, TranslationLanguageEnum lang) {
        return null;
    }

    public String generateSeedListContent(List<String> tournamentIds, TranslationLanguageEnum lang) {

        return null;
    }
}
