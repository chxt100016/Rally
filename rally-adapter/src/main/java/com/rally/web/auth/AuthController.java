package com.rally.web.auth;

import com.rally.auth.AuthAppService;
import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.tour.model.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 与登录渠道无关的认证后用户操作。
 */
@RestController
@RequestMapping({"/auth", "/wechat/auth"})
public class AuthController {

    @Resource
    private AuthAppService authAppService;

    @PostMapping("/complete-registration")
    public Result<Void> completeRegistration(@Valid @RequestBody CompleteRegistrationCmd cmd) {
        authAppService.completeRegistration(cmd);
        return Result.ok();
    }
}
