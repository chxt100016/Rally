package com.rally.web.recap;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreListQueryCmd;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.tour.model.Result;
import com.rally.recap.ScoreAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recap/score")
public class ScoreController {

    @Resource
    private ScoreAppService scoreAppService;

    @PostMapping("/add")
    public Result<?> addScore(@Valid @RequestBody ScoreAddCmd cmd) {
        scoreAppService.addScore(cmd);
        return Result.ok();
    }

    @PostMapping("/update")
    public Result<?> updateScore(@Valid @RequestBody ScoreUpdateCmd cmd) {
        scoreAppService.updateScore(cmd);
        return Result.ok();
    }

    @PostMapping("/delete")
    public Result<?> deleteScore(@Valid @RequestBody ScoreDeleteCmd cmd) {
        scoreAppService.deleteScore(cmd);
        return Result.ok();
    }

    @GetMapping("/stats/me")
    public Result<?> myStats(@RequestParam(value = "matchType", required = false) MatchTypeEnum matchType) {
        return Result.ok(scoreAppService.queryMyScoreStats(matchType));
    }

    @PostMapping("/list/me")
    public Result<?> listMyScores(@RequestBody ScoreListQueryCmd cmd) {
        return Result.ok(scoreAppService.queryMyScores(cmd));
    }

}
