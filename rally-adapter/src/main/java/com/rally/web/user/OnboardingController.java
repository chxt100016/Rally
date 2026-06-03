package com.rally.web.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.TennisProfileVO;
import com.rally.user.OnboardingAppService;
import com.rally.web.auth.UserContext;
import jakarta.annotation.Resource;
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
    public Result<TennisProfileVO> onboardingStatus() {
        try {
            String userId = UserContext.get();
            return Result.ok(onboardingAppService.checkStatus(userId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 提交 Onboarding，转 normal
     */
    @PostMapping("/submit")
    public Result<TennisProfileVO> onboardingSubmit(@RequestBody OnboardingCmd cmd) {
        try {
            String userId = UserContext.get();
            return Result.ok(onboardingAppService.submit(userId, cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
