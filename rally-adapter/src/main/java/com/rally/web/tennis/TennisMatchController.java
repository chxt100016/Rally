package com.rally.web.tennis;

import com.rally.domain.tennis.model.CourtMatchDTO;
import com.rally.domain.tennis.model.MatchQueryVO;
import com.rally.domain.tennis.model.Result;
import com.rally.tennis.MatchQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/matches")
public class TennisMatchController {

    @Resource
    private MatchQueryService matchQueryService;

    @GetMapping("/upcoming")
    public Result<List<CourtMatchDTO>> upcoming(@RequestParam("tournamentId") String tournamentId) {
        return Result.ok(matchQueryService.queryUpcomingByCourt(tournamentId));
    }

    @GetMapping("/finished")
    public Result<List<MatchQueryVO>> finished(@RequestParam("tournamentId") String tournamentId) {
        return Result.ok(matchQueryService.queryFinishedList(tournamentId));
    }
}
