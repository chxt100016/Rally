package com.rally.personalprofile.playerhome.activity;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.user.model.MyProfileUserDTO;
import com.rally.domain.user.model.MyProfileVideoDTO;
import com.rally.domain.user.model.PlayerHomeDTO;
import com.rally.domain.user.model.PlayerHomeLevelDTO;
import com.rally.domain.user.model.PlayerHomeScoreDTO;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.model.VideoItemDTO;
import com.rally.domain.user.model.VideoVO;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务活动 query-player-public-profile：组装目标球员的公开档案分组。
 */
@Component
@RequiredArgsConstructor
public class QueryPlayerPublicProfileActivity {

    private final UserProfileDomainService userProfileDomainService;

    public PlayerHomeDTO execute(String targetUserId) {
        // A1：基础用户必须存在，网球档案仍然可缺省。
        UserProfile userProfile = userProfileDomainService.get(targetUserId);
        TennisProfileData profileData = userProfile.getProfile();

        return new PlayerHomeDTO()
                .setUser(buildUserDTO(userProfile.getUser()))
                .setLevel(buildLevelDTO(profileData))
                .setScore(buildScoreDTO(profileData))
                .setVideo(buildVideoDTO(profileData));
    }

    /** A2：只投影公开字段，不赋城市名称和隐私字段。 */
    private MyProfileUserDTO buildUserDTO(UserData userData) {
        if (userData == null) {
            return new MyProfileUserDTO();
        }
        return new MyProfileUserDTO()
                .setUserId(userData.getUserId())
                .setNickname(userData.getNickname())
                .setAvatarUrl(QiniuConfiguration.buildSignedUrl(userData.getAvatarUrl()))
                .setGender(userData.getGender())
                .setBirthday(userData.getBirthday())
                .setCityCode(userData.getCityCode())
                .setBio(userData.getBio());
    }

    /** A3：无档案时等级字段为空、综合评级为空字符串。 */
    private PlayerHomeLevelDTO buildLevelDTO(TennisProfileData profileData) {
        if (profileData == null) {
            return new PlayerHomeLevelDTO();
        }
        return new PlayerHomeLevelDTO()
                .setNtrpScore(profileData.getNtrpScore())
                .setIsUnderReview(profileData.getIsUnderReview())
                .setIsNewbie(profileData.getIsNewbie());
    }

    private PlayerHomeScoreDTO buildScoreDTO(TennisProfileData profileData) {
        return new PlayerHomeScoreDTO()
                .setProfileLevel(ProfileLevelManager.calculate(profileData));
    }

    /** A4：全量保留视频，公开页不赋上传限制。 */
    private MyProfileVideoDTO buildVideoDTO(TennisProfileData profileData) {
        MyProfileVideoDTO videoDTO = new MyProfileVideoDTO();
        if (profileData == null || profileData.getVideos() == null) {
            return videoDTO
                    .setTotal(0)
                    .setData(new ArrayList<>());
        }

        List<VideoVO> videos = profileData.getVideos();
        List<VideoItemDTO> items = videos.stream()
                .map(video -> new VideoItemDTO()
                        .setKey(video.getKey())
                        .setUrl(QiniuConfiguration.buildSignedUrl(video.getKey()))
                        .setCoverUrl(QiniuConfiguration.buildCover(video.getKey()))
                        .setTitle(StringUtils.isBlank(video.getTitle()) ? "未命名" : video.getTitle()))
                .collect(Collectors.toList());
        return videoDTO
                .setTotal(videos.size())
                .setData(items);
    }
}
