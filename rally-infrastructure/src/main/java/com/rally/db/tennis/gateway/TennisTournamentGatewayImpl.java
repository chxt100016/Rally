package com.rally.db.tennis.gateway;

import com.rally.db.tennis.convert.TennisConvertMapper;
import com.rally.db.tennis.service.TennisTournamentService;
import com.rally.domain.tennis.gateway.TennisTournamentGateway;
import com.rally.domain.tennis.model.TournamentData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TennisTournamentGatewayImpl implements TennisTournamentGateway {

    private final TennisTournamentService tennisTournamentService;
    private static final TennisConvertMapper MAPPER = TennisConvertMapper.INSTANCE;

    @Override
    public List<TournamentData> findCurrentTournaments(LocalDate date) {
        return MAPPER.toTournamentDataList(tennisTournamentService.findCurrentTournaments(date));
    }

    @Override
    public boolean exists(String tournamentId) {
        return tennisTournamentService.existsByTournamentId(tournamentId);
    }

    @Override
    public void saveOrUpdateBatch(List<TournamentData> tournaments) {
        tennisTournamentService.saveOrUpdateBatch(MAPPER.toTournamentPOList(tournaments));
    }

    @Override
    public List<TournamentData> listByCondition(String status, String tour, LocalDate dateFrom, LocalDate dateTo) {
        return MAPPER.toTournamentDataList(tennisTournamentService.listByCondition(status, tour, dateFrom, dateTo));
    }

    @Override
    public void updateImagePaths(String tournamentId, String imagePath, String backgroundPath) {
        tennisTournamentService.updateImagePaths(tournamentId, imagePath, backgroundPath);
    }

    @Override
    public List<TournamentData> listByTournamentIds(List<String> tournamentIds) {
        return MAPPER.toTournamentDataList(tennisTournamentService.listByTournamentIds(tournamentIds));
    }

    @Override
    public TournamentData findByTournamentId(String tournamentId) {
        return MAPPER.toTournamentData(tennisTournamentService.findByTournamentId(tournamentId));
    }

    @Override
    public List<TournamentData> listPendingBackground(LocalDate dateFrom, LocalDate dateTo) {
        return MAPPER.toTournamentDataList(tennisTournamentService.listPendingBackground(dateFrom, dateTo));
    }
}
