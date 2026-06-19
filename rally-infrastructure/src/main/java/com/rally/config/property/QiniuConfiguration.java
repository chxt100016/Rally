package com.rally.config.property;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.util.Auth;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 七牛云配置
 * <p>
 * 从 application.yml 中读取 rally.qiniu.* 配置项。
 * 属性为静态，支持动态刷新（RefreshScope），无需重启即可更新。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "qiniu")
public class QiniuConfiguration {

    @Getter
    private static String accessKey;
    @Getter
    private static String secretKey;
    @Getter
    private static String bucket;
    @Getter
    private static String domain;




    public void setDomain(String domain) {
        QiniuConfiguration.domain = domain;
    }

    public  void setAccessKey(String accessKey) {
        QiniuConfiguration.accessKey = accessKey;
    }

    public  void setSecretKey(String secretKey) {
        QiniuConfiguration.secretKey = secretKey;
    }

    public  void setBucket(String bucket) {
        QiniuConfiguration.bucket = bucket;
    }

    /**
     * 生成公开访问 URL
     * @param key 存储 key
     * @return 完整的 CDN 访问地址
     */
    public static String buildSignedUrl(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
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

    /**
     * 生成封面图 URL，将 key 的后缀替换为 .jpg
     * @param key 存储 key
     * @return 封面图的签名 URL
     */
    public static String buildCover(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String coverKey = key.substring(0, key.lastIndexOf(".")) + ".jpg";
        return buildSignedUrl(coverKey);
    }
}
