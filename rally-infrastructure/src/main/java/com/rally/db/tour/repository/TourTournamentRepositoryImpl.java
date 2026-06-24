package com.rally.db.tour.repository;

import com.rally.db.tour.convert.TourConvertMapper;
import com.rally.db.tour.service.TourTournamentService;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.tour.model.TournamentData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TourTournamentRepositoryImpl implements TourTournamentRepository {

    private final TourTournamentService tourTournamentService;
    private static final TourConvertMapper MAPPER = TourConvertMapper.INSTANCE;

    @Override
    public List<TournamentData> findCurrentTournaments(LocalDate date) {
        return MAPPER.toTournamentDataList(tourTournamentService.findCurrentTournaments(date));
    }

    @Override
    public boolean exists(String tournamentId) {
        return tourTournamentService.existsByTournamentId(tournamentId);
    }

    @Override
    public void saveOrUpdateBatch(List<TournamentData> tournaments) {
        tourTournamentService.saveOrUpdateBatch(MAPPER.toTournamentPOList(tournaments));
    }

    @Override
    public List<TournamentData> listByCondition(String status, String tour, LocalDate dateFrom, LocalDate dateTo) {
        return MAPPER.toTournamentDataList(tourTournamentService.listByCondition(status, tour, dateFrom, dateTo));
    }

    @Override
    public void updateImagePaths(String tournamentId, String imagePath, String backgroundPath) {
        tourTournamentService.updateImagePaths(tournamentId, imagePath, backgroundPath);
    }

    @Override
    public List<TournamentData> listByTournamentIds(List<String> tournamentIds) {
        return MAPPER.toTournamentDataList(tourTournamentService.listByTournamentIds(tournamentIds));
    }

    @Override
    public TournamentData findByTournamentId(String tournamentId) {
        return MAPPER.toTournamentData(tourTournamentService.findByTournamentId(tournamentId));
    }

    @Override
    public List<TournamentData> listPendingBackground(LocalDate dateFrom, LocalDate dateTo) {
        return MAPPER.toTournamentDataList(tourTournamentService.listPendingBackground(dateFrom, dateTo));
    }
}
