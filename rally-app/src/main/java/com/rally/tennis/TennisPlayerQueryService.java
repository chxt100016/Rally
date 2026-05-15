package com.rally.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisPlayerRepository;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.domain.tennis.gateway.MatchQueryGateway;
import com.rally.domain.tennis.model.*;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TennisPlayerQueryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Resource
    private TennisPlayerRepository tennisPlayerRepository;



    public List<PlayerQueryVO> queryPlayers(String tour) {
        if (tour == null || tour.isBlank()) return List.of();
        List<com.rally.db.tennis.entity.TennisPlayerPO> players =
                tennisPlayerRepository.listByTourOrderByRank(tour.toUpperCase());
        LocalDate today = LocalDate.now();
        return players.stream()
                .map(po -> toPlayerQueryVO(po, today))
                .toList();
    }

    private PlayerQueryVO toPlayerQueryVO(com.rally.db.tennis.entity.TennisPlayerPO po, LocalDate today) {
        PlayerQueryVO vo = new PlayerQueryVO();
        vo.setId(po.getPlayerId());
        vo.setRank(po.getRank());
        String first = po.getFirstName() != null ? po.getFirstName() : "";
        String last  = po.getLastName()  != null ? po.getLastName()  : "";
        vo.setName((first + " " + last).trim());
        vo.setCountry(CountryEnum.getCountry(po.getNationality()));
        vo.setPoints(po.getPoints());
        if (po.getBirthDate() != null) {
            vo.setAge(Period.between(po.getBirthDate(), today).getYears());
            vo.setBirthDate(po.getBirthDate().format(DATE_FMT));
        }
        return vo;
    }
}
