package com.rally.wechat.user;

import com.rally.web.user.UserController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/user")
public class WechatUserController extends UserController {

}
