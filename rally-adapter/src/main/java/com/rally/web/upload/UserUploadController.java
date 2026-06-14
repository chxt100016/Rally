package com.rally.web.upload;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.upload.VideoAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/upload")
public class UserUploadController {

    @Resource
    private VideoAppService videoAppService;

    /**
     * 取直传凭证
     */
    @GetMapping("/upload-token/video")
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
    @DeleteMapping("/video")
    public Result<Void> delete(@RequestParam("key") String key) {
        try {
            videoAppService.deleteVideo(key);
            return Result.ok(null);
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 取头像直传凭证
     * @param ext 文件扩展名，如 jpg、png
     */
    @GetMapping("/upload-token/avatar")
    public Result<VideoTokenVO> getAvatarUploadToken(@RequestParam("ext") String ext) {
        try {
            return Result.ok(videoAppService.getAvatarUploadToken(ext));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

}
