package com.rally.upload;

import com.rally.utils.UserContext;
import com.rally.contentproduction.genericimageupload.activity.StoreGenericImageActivity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UploadAppService {

    @Resource
    private StoreGenericImageActivity storeGenericImageActivity;

    /**
     * 上传普通图片
     */
    public String uploadImage(MultipartFile file, String dir, String filename) throws IOException {
        return storeGenericImageActivity.execute(file.getBytes(), dir, filename);
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        String userId = UserContext.get();
        return storeGenericImageActivity.execute(file.getBytes(), "avatar", userId);
    }
}
