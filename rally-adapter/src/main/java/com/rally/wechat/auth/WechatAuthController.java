package com.rally.wechat.auth;

import com.rally.auth.AuthAppService;
import com.rally.domain.auth.model.LoginResultVO;
import com.rally.domain.auth.model.WechatLoginCmd;
import com.rally.domain.tour.model.Result;
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
        return Result.ok(authAppService.login(cmd));
    }
}
