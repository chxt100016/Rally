package com.rally.domain.user.gateway;

import com.rally.domain.user.model.VideoTokenVO;

/**
 * 视频直传凭证签发网关
 */
public interface VideoUploadGateway {

    /**
     * 生成视频直传上传凭证（STS）
     *
     * @param userId    作用域限定到 videos/{userId}/ 前缀
     * @param maxSizeMb 单文件大小上限（fsizeLimit）
     * @return 上传凭证 + 作用域前缀
     */
    VideoTokenVO generateUploadToken(String userId, int maxSizeMb);

    /**
     * 校验七牛回调签名
     *
     * @param authorization Authorization 头
     * @param body          请求体
     * @return 签名是否有效
     */
    boolean verifyCallbackSignature(String authorization, String body);
}
