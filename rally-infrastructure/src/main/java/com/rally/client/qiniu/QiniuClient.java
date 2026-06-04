package com.rally.client.qiniu;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.rally.config.property.QiniuConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class QiniuClient {

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
        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());
        String upToken = auth.uploadToken(QiniuConfiguration.getBucket(), key);
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


}
