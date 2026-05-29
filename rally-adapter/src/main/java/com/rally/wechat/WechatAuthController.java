package com.rally.wechat;

import com.rally.auth.AuthAppService;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.model.LoginResultVO;
import com.rally.domain.auth.model.WechatLoginCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.UserVO;
import com.rally.web.auth.UserContext;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public Result<UserVO> completeRegistration(@RequestBody Map<String, String> params) {
        try {
            String userId = UserContext.get();
            String nickname = params.get("nickname");
            String avatarUrl = params.get("avatarUrl");
            String birthday = params.get("birthday");
            String gender = params.get("gender");
            return Result.ok(authAppService.completeRegistration(userId, nickname, avatarUrl, birthday, gender));
        } catch (AuthException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
