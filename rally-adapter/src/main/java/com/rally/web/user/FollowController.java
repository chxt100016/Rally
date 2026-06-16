package com.rally.web.user;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.FollowCmd;
import com.rally.domain.user.model.FollowListCmd;
import com.rally.domain.user.model.FollowUserDTO;
import com.rally.user.FollowAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/follow")
public class FollowController {

    @Resource
    private FollowAppService followAppService;

    /**
     * 关注用户
     */
    @PostMapping("")
    public Result<Void> follow(@RequestBody @Valid FollowCmd cmd) {
        followAppService.follow(cmd);
        return Result.ok();
    }

    /**
     * 取消关注
     */
    @PostMapping("/cancel")
    public Result<Void> unfollow(@RequestBody @Valid FollowCmd cmd) {
        followAppService.unfollow(cmd);
        return Result.ok();
    }

    /**
     * 关注列表（userId 为空时查询当前登录用户）
     */
    @GetMapping("/following")
    public Result<PageDTO<FollowUserDTO>> following(@Valid FollowListCmd cmd) {
        return Result.ok(followAppService.getFollowingList(cmd));
    }

    /**
     * 被关注（粉丝）列表（userId 为空时查询当前登录用户）
     */
    @GetMapping("/followers")
    public Result<PageDTO<FollowUserDTO>> followers(@Valid FollowListCmd cmd) {
        return Result.ok(followAppService.getFollowerList(cmd));
    }
}
