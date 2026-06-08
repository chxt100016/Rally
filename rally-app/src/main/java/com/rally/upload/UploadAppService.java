package com.rally.upload;

import com.rally.utils.UserContext;
import com.rally.client.qiniu.QiniuClient;
import com.rally.config.property.QiniuConfiguration;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UploadAppService {

    @Resource
    private QiniuClient qiniuClient;

    /**
     * 上传普通图片
     */
    public String uploadImage(MultipartFile file, String dir, String filename) throws IOException {
        String key = qiniuClient.uploadImage(file.getBytes(), dir, filename);
        return QiniuConfiguration.buildSignedUrl(key);
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        String userId = UserContext.get();
        String key = qiniuClient.uploadImage(file.getBytes(), "avatar", userId);
        return QiniuConfiguration.buildSignedUrl(key);
    }
}
