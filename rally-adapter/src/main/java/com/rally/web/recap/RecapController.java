package com.rally.web.recap;

import com.rally.domain.recap.enums.RecapOverallStatus;
import com.rally.domain.recap.model.RecapCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.recap.RecapAppService;
import com.rally.recap.convert.RecapAppConvertMapper;
import com.rally.recap.model.RecapDetailDTO;
import com.rally.recap.model.RecapSubmitDTO;
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

    private static final RecapAppConvertMapper MAPPER = RecapAppConvertMapper.INSTANCE;

    /**
     * 提交赛后收集（比分 + 评价）
     * POST /api/rally/recap/submit
     */
    @PostMapping("/submit")
    public Result<RecapOverallStatus> submit(@Valid @RequestBody RecapSubmitDTO req) {
        // DTO -> Cmd（MapStruct）
        RecapCmd cmd = MAPPER.toCmd(req);
        RecapOverallStatus status = recapAppService.submit(cmd);
        return Result.ok(status);
    }

    /**
     * 查询赛后收集详情
     * GET /api/rally/recap/detail?meetupId=xxx
     */
    @GetMapping("/detail")
    public Result<RecapDetailDTO> detail(@RequestParam("meetupId") String meetupId) {
        return Result.ok(recapAppService.detail(meetupId));
    }
}
