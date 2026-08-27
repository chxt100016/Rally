package com.rally.tour;

import com.rally.contentproduction.dailyschedulecopy.activity.ComposeDailyScheduleCopyActivity;
import com.rally.contentproduction.dailyschedulecopy.activity.ComposeDailyScheduleCopyResult;
import com.rally.contentproduction.dailyschedulecopy.activity.RegisterMissingTranslationsActivity;
import com.rally.contentproduction.seedlistcopy.activity.ComposeSeedListCopyActivity;
import com.rally.contentproduction.seedlistcopy.activity.ComposeSeedListCopyResult;
import com.rally.contentproduction.tournamentposterprompt.activity.GenerateCartoonPosterPromptActivity;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourContentAppService {

    private final GenerateCartoonPosterPromptActivity generateCartoonPosterPromptActivity;
    private final ComposeDailyScheduleCopyActivity composeDailyScheduleCopyActivity;
    private final RegisterMissingTranslationsActivity registerDailyMissingTranslationsActivity;
    private final ComposeSeedListCopyActivity composeSeedListCopyActivity;
    private final com.rally.contentproduction.seedlistcopy.activity.RegisterMissingTranslationsActivity
            registerSeedMissingTranslationsActivity;

    public String generatePosterPrompt(String tournamentId) {
        return generateCartoonPosterPromptActivity.execute(tournamentId);
    }

    public String generateDailyContent() {
        ComposeDailyScheduleCopyResult result = composeDailyScheduleCopyActivity.execute();
        registerDailyMissingTranslationsActivity.execute(result.missingTranslationKeys());
        return result.copy();
    }

    public String generateSeedListContent(List<String> tournamentIds,
                                          TranslationLanguageEnum language) {
        ComposeSeedListCopyResult result = composeSeedListCopyActivity.execute(tournamentIds, language);
        registerSeedMissingTranslationsActivity.execute(result.missingTranslations());
        return result.copy();
    }
}
