package com.rally.web.user;

import com.rally.domain.tour.model.Result;
import com.rally.domain.user.model.*;
import com.rally.user.ProfileAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户视频管理
 */
@RestController
@RequestMapping("/user/profile/video")
public class UserProfileVideoController {

    @Resource
    private ProfileAppService profileAppService;

    /**
     * 上传视频
     */
    @PostMapping("/upload")
    public Result<MyProfileDTO> upload(@RequestBody @Valid UploadVideoCmd cmd) {
        return Result.ok(profileAppService.uploadVideo(cmd));
    }

    /**
     * 删除视频
     */
    @PostMapping("/delete")
    public Result<MyProfileDTO> delete(@RequestBody @Valid DeleteVideoCmd cmd) {
        return Result.ok(profileAppService.deleteVideo(cmd));
    }

    /**
     * 修改视频
     */
    @PostMapping("/update")
    public Result<MyProfileDTO> update(@RequestBody @Valid UpdateVideoCmd cmd) {
        return Result.ok(profileAppService.updateVideo(cmd));
    }
}
