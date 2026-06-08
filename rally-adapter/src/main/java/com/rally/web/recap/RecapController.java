package com.rally.web.recap;

import com.rally.domain.recap.enums.RecapOverallStatus;
import com.rally.domain.recap.model.RecapSubmitCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.recap.RecapAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 赛后收集接口：提交、详情
 */
@RestController
@RequestMapping("/recap")
public class RecapController {

    @Resource
    private RecapAppService recapAppService;

    /**
     * 提交赛后收集（比分 + 评价）
     * POST /api/rally/recap/submit
     */
    @PostMapping("/submit")
    public Result<RecapOverallStatus> submit(@Valid @RequestBody RecapSubmitCmd cmd) {
        return Result.ok(recapAppService.submit(cmd));
    }
}
