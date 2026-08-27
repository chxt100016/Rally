package com.rally.tour;

import com.rally.domain.tour.model.TournamentDTO;
import com.rally.protourdata.tournamentquery.activity.QueryTournamentCatalogActivity;
import com.rally.protourdata.tournamentquery.activity.RegisterMissingTourTranslationsActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentQueryService {

    private final QueryTournamentCatalogActivity queryTournamentCatalogActivity;
    private final RegisterMissingTourTranslationsActivity registerMissingTourTranslationsActivity;

    public List<TournamentDTO> queryTournaments(String status, String type, String range) {
        QueryTournamentCatalogActivity.Result result =
                queryTournamentCatalogActivity.execute(status, type, range);
        return registerMissingTourTranslationsActivity.execute(
                result.tournaments(), result.translationKeys());
    }
}
