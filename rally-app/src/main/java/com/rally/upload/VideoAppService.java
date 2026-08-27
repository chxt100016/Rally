package com.rally.upload;

import com.rally.utils.UserContext;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.personalprofile.profilevideodelete.activity.DeleteUploadedVideoFileActivity;
import com.rally.personalprofile.profilevideodelete.activity.RemoveUploadedVideoItemsActivity;
import com.rally.personalprofile.userimageuploadauthorization.activity.IssueAvatarUploadAuthorizationActivity;
import com.rally.personalprofile.userimageuploadauthorization.activity.IssueUserImageUploadAuthorizationActivity;
import com.rally.personalprofile.videouploadauthorization.activity.IssueVideoUploadAuthorizationActivity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VideoAppService {

    @Resource
    private IssueVideoUploadAuthorizationActivity issueVideoUploadAuthorizationActivity;

    @Resource
    private IssueAvatarUploadAuthorizationActivity issueAvatarUploadAuthorizationActivity;

    @Resource
    private IssueUserImageUploadAuthorizationActivity issueUserImageUploadAuthorizationActivity;

    @Resource
    private RemoveUploadedVideoItemsActivity removeUploadedVideoItemsActivity;

    @Resource
    private DeleteUploadedVideoFileActivity deleteUploadedVideoFileActivity;

    public VideoTokenVO getVideoUploadToken() {
        return issueVideoUploadAuthorizationActivity.execute(UserContext.get());
    }

    @Transactional
    public void deleteVideo(String key) {
        String userId = UserContext.get();
        removeUploadedVideoItemsActivity.execute(userId, key);
        deleteUploadedVideoFileActivity.execute(key);
    }

    public VideoTokenVO getAvatarUploadToken(String ext) {
        return issueAvatarUploadAuthorizationActivity.execute(UserContext.get(), ext);
    }

    public VideoTokenVO getUserFileUploadToken(String type) {
        return issueUserImageUploadAuthorizationActivity.execute(UserContext.get(), type);
    }
}
