package com.rally.user;

import com.rally.domain.user.model.MyProfileDTO;
import com.rally.personalprofile.basicprofileupdate.activity.AssembleMyProfileActivity;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 我的档案应用服务
 * 负责 getMyProfile 及其子 DTO 构建
 */
@Service
public class MyProfileAppService {

    @Resource
    private AssembleMyProfileActivity assembleMyProfileActivity;

    /**
     * 我的档案
     */
    public MyProfileDTO getMyProfile() {
        String userId = UserContext.get();
        return assembleMyProfileActivity.execute(userId);
    }
}
