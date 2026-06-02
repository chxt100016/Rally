package com.rally.upload;

import com.rally.client.qiniu.QiniuClient;
import com.qiniu.common.QiniuException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UploadAppService {

    @Resource
    private QiniuClient qiniuClient;

    public String uploadImage(MultipartFile file, String dir, String filename) throws IOException {
        String key = qiniuClient.uploadImage(file.getBytes(), dir, filename);
        return qiniuClient.buildSignedUrl(key);
    }
}
