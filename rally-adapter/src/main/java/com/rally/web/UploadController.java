package com.rally.web;

import com.rally.upload.UploadAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/upload")
public class UploadController {

    @Resource
    private UploadAppService uploadAppService;

    @PostMapping("/image")
    public Map<String, String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", required = false) String dir,
            @RequestParam(value = "filename", required = false) String filename) throws Exception {
        String url = uploadAppService.uploadImage(file, dir, filename);
        return Map.of("url", url);
    }
}
