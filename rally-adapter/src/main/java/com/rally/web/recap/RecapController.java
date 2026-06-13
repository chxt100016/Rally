package com.rally.web.recap;

import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreSubmitCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.recap.RecapAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛后收集接口：提交比分、提交评价
 */
@RestController
@RequestMapping("/wechat/recap")
public class RecapController {

    @Resource
    private RecapAppService recapAppService;

    /**
     * 提交比分
     */
    @PostMapping("/score")
    public Result<?> submitScore(@Valid @RequestBody ScoreSubmitCmd cmd) {
        recapAppService.submitScore(cmd);
        return Result.ok();
    }

    /**
     * 提交评价
     */
    @PostMapping("/review")
    public Result<?> submitReview(@Valid @RequestBody ReviewSubmitCmd cmd) {
        recapAppService.submitReview(cmd);
        return Result.ok();
    }
}
