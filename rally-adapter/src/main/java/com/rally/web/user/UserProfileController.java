package com.rally.web.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tour.model.Result;
import com.rally.domain.user.model.*;
import com.rally.user.MyProfileAppService;
import com.rally.user.PlayerHomeAppService;
import com.rally.user.ProfileAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/profile")
public class UserProfileController {

    @Resource
    private ProfileAppService profileAppService;

    @Resource
    private MyProfileAppService myProfileAppService;

    @Resource
    private PlayerHomeAppService playerHomeAppService;

    /**
     * 我的信息
     */
    @GetMapping("/me")
    public Result<MyProfileDTO> me() {
       return Result.ok(myProfileAppService.getMyProfile());
    }

    /**
     * 球员主页
     */
    @GetMapping("/{userId}")
    public Result<PlayerHomeDTO> playerHome(@PathVariable("userId") String userId) {
        return Result.ok(playerHomeAppService.getPlayerHome(userId));
    }

    /**
     * 编辑资料
     */
    @PutMapping("")
    public Result<MyProfileDTO> editProfile(@RequestBody EditProfileCmd cmd) {
        try {
            return Result.ok(profileAppService.editUser(cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 修改性别
     */
    @PutMapping("/gender")
    public Result<MyProfileDTO> updateGender(@RequestBody @Valid UpdateGenderCmd cmd) {
        return Result.ok(profileAppService.updateGender(cmd));
    }

    /**
     * 自评修改
     */
    @PutMapping("/ntrp")
    public Result<MyProfileDTO> updateNtrp(@RequestBody @Valid NtrpUpdateCmd cmd) {
        return Result.ok(profileAppService.updateNtrp(cmd));
    }
}
