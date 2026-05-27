package com.rally.web.tennis;

import com.rally.domain.tennis.model.TournamentPromptVO;
import com.rally.tennis.TennisPromptService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tennis/prompt")
public class TennisPromptController {

    @Resource
    private TennisPromptService tennisPromptService;

    @GetMapping(value = "/tournament", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tournament(@RequestParam("tournamentId") String tournamentId) {
        return tennisPromptService.generatePrompt(tournamentId);
    }

    @GetMapping(value = "/tournaments/pending", produces = MediaType.TEXT_PLAIN_VALUE)
    public String pending() {
        List<TournamentPromptVO> data = tennisPromptService.listPendingPrompts();
        return data.stream()
                .map(TournamentPromptVO::getPrompt)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
