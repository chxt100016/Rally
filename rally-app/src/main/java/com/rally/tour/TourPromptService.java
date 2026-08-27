package com.rally.tour;

import com.rally.contentproduction.pendingposterlist.activity.ListPendingPosterPromptsActivity;
import com.rally.contentproduction.tournamentposterprompt.activity.GenerateStandardPosterPromptActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourPromptService {

    private final GenerateStandardPosterPromptActivity generateStandardPosterPromptActivity;
    private final ListPendingPosterPromptsActivity listPendingPosterPromptsActivity;

    public String generatePrompt(String tournamentId) {
        return generateStandardPosterPromptActivity.execute(tournamentId);
    }

    public String listPendingPrompts() {
        return listPendingPosterPromptsActivity.execute();
    }
}
