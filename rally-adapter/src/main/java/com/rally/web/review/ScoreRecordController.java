package com.rally.web.review;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.review.model.ScoreRecordCmd;
import com.rally.domain.review.model.ScoreRecordData;
import com.rally.domain.tennis.model.Result;
import com.rally.review.ScoreRecordAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 比分接口：记/改盘、删盘、查比分
 */
@RestController
@RequestMapping("/wechat/score")
public class ScoreRecordController {

    @Resource
    private ScoreRecordAppService scoreRecordAppService;

    /**
     * 记比分（新增/修改盘）
     * POST /api/rally/wechat/score/save
     */
    @PostMapping("/save")
    public Result<ScoreRecordData> save(@Valid @RequestBody ScoreRecordCmd cmd) {
        try {
            return Result.ok(scoreRecordAppService.saveScore(cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 删除某盘比分
     * POST /api/rally/wechat/score/delete
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        try {
            String bizId = (String) body.get("bizId");
            Integer version = body.get("version") != null ?
                    ((Number) body.get("version")).intValue() : null;
            scoreRecordAppService.deleteScore(bizId, version);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 获取某场比分（按 set_number 升序）
     * GET /api/rally/wechat/score/list?rallyMeetupId=xxx
     */
    @GetMapping("/list")
    public Result<List<ScoreRecordData>> list(@RequestParam("rallyMeetupId") String rallyMeetupId) {
        try {
            return Result.ok(scoreRecordAppService.listScores(rallyMeetupId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
