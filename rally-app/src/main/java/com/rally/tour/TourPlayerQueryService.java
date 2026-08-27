package com.rally.tour;

import com.rally.domain.tour.model.PlayerQueryVO;
import com.rally.protourdata.playerquery.activity.QueryRankedTourPlayersActivity;
import com.rally.protourdata.playerquery.activity.RegisterMissingTourTranslationsActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourPlayerQueryService {

    private final QueryRankedTourPlayersActivity queryRankedTourPlayersActivity;
    private final RegisterMissingTourTranslationsActivity registerMissingTourTranslationsActivity;

    public List<PlayerQueryVO> queryPlayers(String tour) {
        QueryRankedTourPlayersActivity.Result result = queryRankedTourPlayersActivity.execute(tour);
        registerMissingTourTranslationsActivity.execute(result.missingTranslationKeys());
        return result.players();
    }
}
