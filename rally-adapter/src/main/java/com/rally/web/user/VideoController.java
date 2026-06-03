package com.rally.web.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.VideoCallbackCmd;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.user.ProfileAppService;
import com.rally.web.auth.UserContext;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/video")
public class VideoController {

    @Resource
    private ProfileAppService profileAppService;

    /**
     * 取直传凭证
     */
    @PostMapping("/upload-token")
    public Result<VideoTokenVO> getUploadToken() {
        try {
            String userId = UserContext.get();
            return Result.ok(profileAppService.getVideoUploadToken(userId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 七牛回调入库（白名单，需校验签名）
     */
    @PostMapping("/callback")
    public Result<Void> videoCallback(@RequestBody VideoCallbackCmd cmd,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            profileAppService.handleVideoCallback(cmd);
            return Result.ok(null);
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 删除视频
     */
    @DeleteMapping("")
    public Result<Void> deleteVideo(@RequestParam String key) {
        try {
            String userId = UserContext.get();
            profileAppService.deleteVideo(userId, key);
            return Result.ok(null);
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
