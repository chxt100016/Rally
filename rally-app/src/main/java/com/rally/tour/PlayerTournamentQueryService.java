package com.rally.tour;

import com.rally.domain.tour.model.PlayerTournamentVO;
import com.rally.protourdata.playertournamentpathquery.activity.QueryPlayerTournamentPathActivity;
import com.rally.protourdata.playertournamentpathquery.activity.RegisterMissingTourTranslationsActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerTournamentQueryService {

    private final QueryPlayerTournamentPathActivity queryPlayerTournamentPathActivity;
    private final RegisterMissingTourTranslationsActivity registerMissingTourTranslationsActivity;

    public PlayerTournamentVO query(String tournamentId,
                                    Integer year,
                                    String playerId,
                                    String drawType) {
        QueryPlayerTournamentPathActivity.Result result =
                queryPlayerTournamentPathActivity.execute(tournamentId, year, playerId, drawType);
        registerMissingTourTranslationsActivity.execute(result.missingTranslationKeys());
        return result.data();
    }
}
