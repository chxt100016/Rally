package com.rally.web.user;

import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.TennisProfileVO;
import com.rally.user.OnboardingAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/onboarding")
public class OnboardingController {

    @Resource
    private OnboardingAppService onboardingAppService;

    /**
     * 查是否需引导（无记录则生成 tbc）
     */
    @GetMapping("/status")
    public Result<?> onboardingStatus() {
        return Result.ok(onboardingAppService.checkStatus());
    }

    /**
     * 提交 Onboarding，转 normal
     */
    @PostMapping("/submit")
    public Result<TennisProfileVO> onboardingSubmit(@Valid @RequestBody OnboardingCmd cmd) {
        return Result.ok(onboardingAppService.submit(cmd));
    }
}
