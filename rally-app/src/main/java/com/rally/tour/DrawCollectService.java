package com.rally.tour;

import com.rally.domain.tour.repository.TourDrawRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DrawCollectService {

    @Resource
    private TourDrawRepository tourDrawRepository;

    public Long saveOrUpdate(String tournamentId, int year, String drawTypeCode, Integer drawSize, Integer totalRounds) {
        return tourDrawRepository.saveOrUpdate(tournamentId, year, drawTypeCode, drawSize, totalRounds);
    }
}
