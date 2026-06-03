package com.rally.wechat.user;

import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.UserVO;
import com.rally.user.UserQueryService;
import com.rally.web.auth.UserContext;
import com.rally.web.user.UserController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/user")
public class WechatUserController extends UserController {

}
