package com.rally.web.tennis;

import com.rally.domain.tennis.model.Result;
import com.rally.domain.tennis.model.TournamentPromptVO;
import com.rally.tennis.TennisPromptService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tennis/prompt")
public class TennisPromptController {

    @Resource
    private TennisPromptService tennisPromptService;

    @GetMapping("/tournament")
    public Result<String> tournament(@RequestParam("tournamentId") String tournamentId) {
        String prompt = tennisPromptService.generatePrompt(tournamentId);
        return Result.ok(prompt);
    }

    @GetMapping("/tournaments/pending")
    public Result<List<TournamentPromptVO>> pending() {
        List<TournamentPromptVO> data = tennisPromptService.listPendingPrompts();
        return Result.ok(data);
    }
}
