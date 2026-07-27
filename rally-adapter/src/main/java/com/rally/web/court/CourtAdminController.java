package com.rally.web.court;

import com.rally.court.CourtUploadAppService;
import com.rally.domain.tour.model.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 球场管理（运营后台）接口：上传球场背景图
 */
@RestController
@RequestMapping("/court/admin")
public class CourtAdminController {

    @Resource
    private CourtUploadAppService courtUploadAppService;

    /**
     * 上传球场背景图，压缩后存储于 court/{courtId}/backgroundImage.jpeg
     */
    @PostMapping("/uploadBackgroundImage")
    public Result<Map<String, String>> uploadBackgroundImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courtId") String courtId) throws Exception {
        String key = courtUploadAppService.uploadBackgroundImage(file, courtId);
        return Result.ok(Map.of("backgroundImage", key));
    }
}
