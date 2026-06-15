package com.rally.web.recap;

import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.recap.RecapAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛后收集接口：比分增删改、提交评价
 */
@RestController
@RequestMapping("/wechat/recap")
public class RecapController {

    @Resource
    private RecapAppService recapAppService;

    /**
     * 新增比分（一次一盘）
     */
    @PostMapping("/score/add")
    public Result<?> addScore(@Valid @RequestBody ScoreAddCmd cmd) {
        recapAppService.addScore(cmd);
        return Result.ok();
    }

    /**
     * 修改比分（一次一盘，bizId 定位 + version 乐观锁）
     */
    @PostMapping("/score/update")
    public Result<?> updateScore(@Valid @RequestBody ScoreUpdateCmd cmd) {
        recapAppService.updateScore(cmd);
        return Result.ok();
    }

    /**
     * 删除比分（一次一盘，bizId 定位）
     */
    @PostMapping("/score/delete")
    public Result<?> deleteScore(@Valid @RequestBody ScoreDeleteCmd cmd) {
        recapAppService.deleteScore(cmd);
        return Result.ok();
    }

    /**
     * 提交评价（一次评价一个用户）
     */
    @PostMapping("/review")
    public Result<?> submitReview(@Valid @RequestBody ReviewSubmitCmd cmd) {
        recapAppService.submitReview(cmd);
        return Result.ok();
    }
}
