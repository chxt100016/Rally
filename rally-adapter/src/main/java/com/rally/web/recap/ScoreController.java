package com.rally.web.recap;

import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreListQueryCmd;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.tour.model.Result;
import com.rally.recap.ScoreAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/recap/score")
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

    @PostMapping("/list")
    public Result<?> listMyScores(@RequestBody ScoreListQueryCmd cmd) {
        return Result.ok(scoreAppService.queryMyScores(cmd));
    }
}
