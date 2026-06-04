package com.rally.wechat.auth;

import com.rally.auth.AuthAppService;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.auth.model.LoginResultVO;
import com.rally.domain.auth.model.WechatLoginCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.UserVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/auth")
public class WechatAuthController {

    @Resource
    private AuthAppService authAppService;

    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody WechatLoginCmd cmd) {
        try {
            return Result.ok(authAppService.login(cmd));
        } catch (AuthException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    @PostMapping("/complete-registration")
    public Result<UserVO> completeRegistration(@RequestBody CompleteRegistrationCmd cmd) {
        try {
            return Result.ok(authAppService.completeRegistration(
                    cmd.getNickname(), cmd.getAvatarUrl(), cmd.getBirthday(), cmd.getGender()));
        } catch (AuthException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
