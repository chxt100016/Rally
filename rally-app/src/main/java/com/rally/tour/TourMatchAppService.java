package com.rally.tour;

import com.rally.domain.tour.model.SeedGroupDTO;
import com.rally.domain.tour.model.TourMatchDTO;
import com.rally.protourdata.finishedmatchesquery.activity.QueryFinishedRoundGroupsActivity;
import com.rally.protourdata.finishedmatchesquery.activity.QueryFinishedSeedGroupsActivity;
import com.rally.protourdata.upcomingmatchesquery.activity.QueryUpcomingMatchGroupsActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourMatchAppService {

    private final QueryUpcomingMatchGroupsActivity queryUpcomingMatchGroupsActivity;
    private final com.rally.protourdata.upcomingmatchesquery.activity.RegisterMissingTourTranslationsActivity
            registerUpcomingTranslationsActivity;
    private final QueryFinishedSeedGroupsActivity queryFinishedSeedGroupsActivity;
    private final QueryFinishedRoundGroupsActivity queryFinishedRoundGroupsActivity;
    private final com.rally.protourdata.finishedmatchesquery.activity.RegisterMissingTourTranslationsActivity
            registerFinishedTranslationsActivity;

    @Cacheable(value = "upcoming", key = "#p0")
    public TourMatchDTO upcoming(List<String> tournamentIds) {
        QueryUpcomingMatchGroupsActivity.Result result =
                queryUpcomingMatchGroupsActivity.execute(tournamentIds);
        registerUpcomingTranslationsActivity.execute(result.missingTranslationKeys());
        return result.data();
    }

    @Cacheable(value = "finished", key = "#p0")
    public TourMatchDTO finished(List<String> tournamentIds) {
        List<SeedGroupDTO> seeds = queryFinishedSeedGroupsActivity.execute(tournamentIds);
        QueryFinishedRoundGroupsActivity.Result result =
                queryFinishedRoundGroupsActivity.execute(tournamentIds, seeds);
        registerFinishedTranslationsActivity.execute(result.missingTranslationKeys());
        return result.data();
    }
}
