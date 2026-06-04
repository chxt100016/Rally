package com.rally.web.upload;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.upload.VideoAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/video")
public class VideoController {

    @Resource
    private VideoAppService videoAppService;

    /**
     * 取直传凭证
     */
    @PostMapping("/upload-token")
    public Result<VideoTokenVO> getUploadToken() {
        try {
            return Result.ok(videoAppService.getVideoUploadToken());
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 删除视频
     */
    @DeleteMapping("")
    public Result<Void> deleteVideo(@RequestParam("key") String key) {
        try {
            videoAppService.deleteVideo(key);
            return Result.ok(null);
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
