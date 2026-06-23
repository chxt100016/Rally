package com.rally.web.content;

import com.rally.domain.tour.model.TournamentPromptVO;
import com.rally.tour.TourPromptService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tour/prompt")
public class TourPromptController {

    @Resource
    private TourPromptService tourPromptService;

    @GetMapping(value = "/tournament", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tournament(@RequestParam("tournamentId") String tournamentId) {
        return tourPromptService.generatePrompt(tournamentId);
    }

    @GetMapping(value = "/tournaments/pending", produces = MediaType.TEXT_PLAIN_VALUE)
    public String pending() {
        List<TournamentPromptVO> data = tourPromptService.listPendingPrompts();
        return data.stream()
                .map(TournamentPromptVO::getPrompt)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
