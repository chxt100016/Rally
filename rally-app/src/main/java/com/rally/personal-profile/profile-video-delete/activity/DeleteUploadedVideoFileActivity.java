package com.rally.personalprofile.profilevideodelete.activity;

import com.rally.client.qiniu.QiniuClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 delete-uploaded-video-file：删除已通过本人视频目录校验的上传文件。
 */
@Component
@RequiredArgsConstructor
public class DeleteUploadedVideoFileActivity {

    private final QiniuClient qiniuClient;

    public void execute(String key) {
        // A1 上游已完成 videos/{userId}/ 前缀校验；此处原样删除，612 由客户端视为成功。
        qiniuClient.deleteFile(key);
        // A2 事务由上传删除入口提交；其他异常继续传播并回滚此前的视频列表更新。
    }
}
