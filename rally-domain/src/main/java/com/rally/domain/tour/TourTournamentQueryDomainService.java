package com.rally.domain.tour;

import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TourTournamentQueryDomainService {

    @Resource
    private TourTournamentRepository tourTournamentRepository;

    public List<TournamentData> findValidCurrentTournaments(LocalDate date) {
        List<TournamentData> tournaments = tourTournamentRepository.findCurrentTournaments(date);
        return tournaments.stream().filter(data -> isCategoryKept(data.getCategory())).toList();
    }

    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) return true;
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
