package com.rally.web.user;

import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.UserVO;
import com.rally.user.UserQueryService;
import com.rally.web.auth.UserContext;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserQueryService userQueryService;

    @GetMapping("/me")
    public Result<UserVO> me() {
        try {
            String userId = UserContext.get();
            return Result.ok(userQueryService.getCurrentUser(userId));
        } catch (AuthException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
