package com.rally.contentproduction.tournamentimagemaintenance.activity;

import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.content.imageasset.TournamentImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 使用项目既有七牛客户端按领域层给定的完整固定键覆盖保存。 */
@Component
@RequiredArgsConstructor
public class TournamentImageStorageAdapter implements TournamentImageStorage {

    private final QiniuClient qiniuClient;

    @Override
    public void overwrite(String key, byte[] content) throws Exception {
        int separator = key.lastIndexOf('/');
        String directory = separator < 0 ? null : key.substring(0, separator);
        String filename = separator < 0 ? key : key.substring(separator + 1);
        qiniuClient.uploadImage(content, directory, filename);
    }
}
