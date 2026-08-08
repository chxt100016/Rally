package com.rally.wechat.user;

import com.rally.domain.tour.model.Result;
import com.rally.user.UserAppService;
import com.rally.user.model.WechatPhoneCodeCmd;
import com.rally.web.user.UserController;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/user")
public class WechatUserController extends UserController {

    @Resource
    private UserAppService userAppService;

    @PostMapping("/phone")
    public Result<Void> savePhone(@Valid @RequestBody WechatPhoneCodeCmd cmd) {
        userAppService.saveWechatPhone(cmd);
        return Result.ok();
    }
}
