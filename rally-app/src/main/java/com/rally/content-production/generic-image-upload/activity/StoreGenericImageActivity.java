package com.rally.contentproduction.genericimageupload.activity;

import com.qiniu.common.QiniuException;
import com.rally.client.qiniu.QiniuClient;
import com.rally.config.property.QiniuConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务活动 store-generic-image：上传一张普通图片并签发一小时有效的访问地址。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreGenericImageActivity {

    private final QiniuClient qiniuClient;

    public String execute(byte[] fileBytes, String dir, String filename) throws QiniuException {
        // A1/A2/A3 沿用七牛客户端既有的魔数识别、资源键生成与完整字节上传语义。
        String key = qiniuClient.uploadImage(fileBytes, dir, filename);

        try {
            // A4 仅在上传成功后签发一小时访问地址；失败时不补偿删除已上传对象。
            return QiniuConfiguration.buildSignedUrl(key);
        } catch (RuntimeException exception) {
            log.error("签发普通图片访问地址失败，已上传对象需另行清理: key={}", key, exception);
            throw exception;
        }
    }
}
