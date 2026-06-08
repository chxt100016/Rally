package com.rally.web;

import com.rally.db.user.entity.UserPO;
import com.rally.db.user.mapper.UserMapper;
import com.rally.domain.utils.ImageCompressor;
import com.rally.utils.TokenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/hello")
    public String hello() {
        return "hello world";
    }

    /**
     * 压缩图片到指定大小
     *
     * @param file     上传的图片文件
     * @param targetKb 目标大小（KB），默认 200
     */
    @PostMapping("/compress-image")
    public ResponseEntity<byte[]> compressImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetKb", defaultValue = "200") int targetKb) throws Exception {

        String originalName = file.getOriginalFilename();
        String format = "jpg";
        if (originalName != null && originalName.contains(".")) {
            format = originalName.substring(originalName.lastIndexOf('.') + 1);
        }

        long originalSize = file.getSize();
        byte[] compressed = ImageCompressor.compress(file.getInputStream(), format, targetKb);

        log.info("图片压缩：原始 {}KB -> 压缩后 {}KB（目标 {}KB）",
                originalSize / 1024, compressed.length / 1024, targetKb);

        String downloadName = "compressed_" + originalName;
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName);
        headers.setContentLength(compressed.length);

        return ResponseEntity.ok().headers(headers).body(compressed);
    }

    @Resource
    private UserMapper userMapper;

    /**
     * 测试接口：查询所有用户并生成token
     */
    @GetMapping("/tokens")
    public Map<String, String> getAllUserTokens() {
        List<UserPO> users = userMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (UserPO user : users) {
            String token = TokenUtils.issue(user.getUserId());
            result.put(user.getUserId(), token);
        }
        return result;

    }
}
