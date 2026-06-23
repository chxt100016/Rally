package com.rally.web.tennis;

import com.rally.domain.tennis.model.Result;
import com.rally.domain.tennis.model.TennisMatchDTO;
import com.rally.tennis.TennisMatchAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tennis/match")
public class TennisMatchController {

    @Resource
    private TennisMatchAppService tennisMatchAppService;

    @GetMapping("/upcoming")
    public Result<TennisMatchDTO> upcoming(@RequestParam("tournamentIds") List<String> tournamentIds) {
        return Result.ok(tennisMatchAppService.upcoming(tournamentIds));
    }

    @GetMapping("/finished")
    public Result<TennisMatchDTO> finished(@RequestParam("tournamentIds") List<String> tournamentIds) {
        return Result.ok(tennisMatchAppService.finished(tournamentIds));
    }
}
