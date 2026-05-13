package com.rally.web.tennis;

import com.rally.domain.tennis.model.MatchQueryVO;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.tennis.model.TournamentQueryVO;
import com.rally.tennis.TennisQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/query")
public class TennisQueryController {

    @Resource
    private TennisQueryService tennisQueryService;

    @GetMapping("/tournaments")
    public Result<List<TournamentQueryVO>> tournaments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "range", required = false) String range) {
        List<TournamentQueryVO> data = tennisQueryService.queryTournaments(status, type, range);
        return Result.ok(data);
    }

    /**
     * 查询比赛列表，按状态分类返回
     * @param tournamentId tournamentId，多个用逗号分隔
     */
    @GetMapping("/matches")
    public Result<Map<String, List<MatchQueryVO>>> matches(
            @RequestParam(value = "tournamentId") String tournamentId) {
        Map<String, List<MatchQueryVO>> data = tennisQueryService.queryMatches(tournamentId);
        return Result.ok(data);
    }
}
