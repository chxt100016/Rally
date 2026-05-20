package com.rally.client.qiniu;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class QiniuClient {

    @Value("${qiniu.access-key}")
    private String accessKey;

    @Value("${qiniu.secret-key}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.domain}")
    private String domain;

    private final UploadManager uploadManager;

    public QiniuClient() {
        Configuration cfg = new Configuration(Region.autoRegion());
        this.uploadManager = new UploadManager(cfg);
    }

    public String uploadImage(byte[] data, String dir, String filename) throws QiniuException {
        String ext = detectExt(data);
        String key;
        if (filename != null && !filename.isBlank()) {
            key = filename.contains(".") ? filename : filename + "." + ext;
        } else {
            key = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        }
        if (dir != null && !dir.isBlank()) {
            key = dir.replaceAll("^/+|/+$", "") + "/" + key;
        }
        log.info("qiniu upload: key={}", key);
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        Response response = uploadManager.put(data, key, upToken);
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
        return putRet.key;
    }

    // detect image format by magic bytes, fallback to jpg
    private String detectExt(byte[] data) {
        if (data.length >= 4) {
            if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) return "jpg";
            if (data[0] == (byte) 0x89 && data[1] == 0x50) return "png";
            if (data[0] == 0x47 && data[1] == 0x49) return "gif";
            if (data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46) return "webp";
        }
        return "jpg";
    }

    public String buildSignedUrl(String key) {
        boolean useHttps = domain.startsWith("https");
        String domainHost = domain.replaceFirst("^https?://", "");
        DownloadUrl downloadUrl = new DownloadUrl(domainHost, useHttps, key);
        long deadline = System.currentTimeMillis() / 1000 + 3600;
        Auth auth = Auth.create(accessKey, secretKey);
        try {
            return downloadUrl.buildURL(auth, deadline);
        } catch (QiniuException e) {
            throw new RuntimeException("构建签名URL失败: " + key, e);
        }
    }
}
