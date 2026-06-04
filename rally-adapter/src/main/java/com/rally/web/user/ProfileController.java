package com.rally.web.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.EditProfileCmd;
import com.rally.domain.user.model.NtrpUpdateCmd;
import com.rally.domain.user.model.PlayerHomeVO;
import com.rally.domain.user.model.TennisProfileVO;
import com.rally.user.ProfileAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/profile")
public class ProfileController {

    @Resource
    private ProfileAppService profileAppService;

    /**
     * 我的档案
     */
    @GetMapping("/me")
    public Result<TennisProfileVO> myProfile() {
        try {
            return Result.ok(profileAppService.getMyProfile());
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 球员主页
     */
    @GetMapping("/{userId}")
    public Result<PlayerHomeVO> playerHome(@PathVariable String userId) {
        try {
            return Result.ok(profileAppService.getPlayerHome(userId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 编辑资料
     */
    @PutMapping("")
    public Result<TennisProfileVO> editProfile(@RequestBody EditProfileCmd cmd) {
        try {
            return Result.ok(profileAppService.editProfile(cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 自评修改
     */
    @PutMapping("/ntrp")
    public Result<TennisProfileVO> updateNtrp(@RequestBody NtrpUpdateCmd cmd) {
        try {
            return Result.ok(profileAppService.updateNtrp(cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
