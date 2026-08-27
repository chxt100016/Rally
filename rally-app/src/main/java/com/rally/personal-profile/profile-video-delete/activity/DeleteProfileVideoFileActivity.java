package com.rally.personalprofile.profilevideodelete.activity;

import com.rally.client.qiniu.QiniuClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 delete-profile-video-file：将请求 key 原样交给七牛执行物理删除。
 */
@Component
@RequiredArgsConstructor
public class DeleteProfileVideoFileActivity {

    private final QiniuClient qiniuClient;

    public void execute(String key) {
        // A1 不校验目录、归属、资源类型或登记状态，key 原样传入。
        // A2 QiniuClient 将 612 视为已删除；其他异常继续向上传播。
        qiniuClient.deleteFile(key);
    }
}
