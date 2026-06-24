package com.rally.tour;

import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.domain.tour.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TourPlayerQueryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private TourPlayerRepository tourPlayerRepository;

    @Resource
    private TourTranslationService tourTranslationService;

    public List<PlayerQueryVO> queryPlayers(String tour) {
        if (tour == null || tour.isBlank()) return List.of();
        List<PlayerData> players = tourPlayerRepository.listByTourOrderByRank(tour.toUpperCase());
        LocalDate today = LocalDate.now();
        List<PlayerQueryVO> result = players.stream()
                .map(p -> toPlayerQueryVO(p, today))
                .toList();
        tourTranslationService.players(result, TranslationLanguageEnum.ZH_CN);
        return result;
    }

    private PlayerQueryVO toPlayerQueryVO(PlayerData player, LocalDate today) {
        PlayerQueryVO vo = new PlayerQueryVO();
        vo.setId(player.getPlayerId());
        vo.setRank(player.getRank());
        String first = player.getFirstName() != null ? player.getFirstName() : "";
        String last  = player.getLastName()  != null ? player.getLastName()  : "";
        vo.setName((first + " " + last).trim());
        vo.setCountry(CountryEnum.getCountry(player.getNationality()));
        vo.setPoints(player.getPoints());
        if (player.getBirthDate() != null) {
            vo.setAge(Period.between(player.getBirthDate(), today).getYears());
            vo.setBirthDate(player.getBirthDate().format(DATE_FMT));
        }
        return vo;
    }
}
