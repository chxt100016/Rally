package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.user.model.FollowCmd;
import com.rally.domain.user.model.FollowListCmd;
import com.rally.domain.user.model.FollowUserDTO;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserFollowData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserFollowDomainService;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 关注应用服务
 */
@Slf4j
@Service
public class FollowAppService {

    @Resource
    private UserFollowDomainService userFollowDomainService;

    @Resource
    private UserProfileDomainService userProfileDomainService;

    /** 关注 */
    public void follow(FollowCmd cmd) {
        userFollowDomainService.follow(UserContext.get(), cmd.getTargetUserId());
    }

    /** 取消关注 */
    public void unfollow(FollowCmd cmd) {
        userFollowDomainService.unfollow(UserContext.get(), cmd.getTargetUserId());
    }

    /** 关注列表（取对端 followingId） */
    public PageDTO<FollowUserDTO> getFollowingList(FollowListCmd cmd) {
        String targetUserId = StringUtils.isNotBlank(cmd.getUserId()) ? cmd.getUserId() : UserContext.get();
        PageDTO<UserFollowData> page = userFollowDomainService.listFollowing(targetUserId, cmd);
        return enrich(page, UserFollowData::getFollowingId);
    }

    /** 被关注列表（取对端 followerId） */
    public PageDTO<FollowUserDTO> getFollowerList(FollowListCmd cmd) {
        String targetUserId = StringUtils.isNotBlank(cmd.getUserId()) ? cmd.getUserId() : UserContext.get();
        PageDTO<UserFollowData> page = userFollowDomainService.listFollowers(targetUserId, cmd);
        return enrich(page, UserFollowData::getFollowerId);
    }

    /** 批量补全用户信息并标记当前登录用户是否已关注 */
    private PageDTO<FollowUserDTO> enrich(PageDTO<UserFollowData> page, Function<UserFollowData, String> otherSide) {
        List<UserFollowData> rows = page.getList();
        if (rows.isEmpty()) {
            return new PageDTO<>(List.of(), null, page.getHasMore());
        }
        List<String> userIds = rows.stream().map(otherSide).collect(Collectors.toList());
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(userIds);
        Set<String> followedSet = userFollowDomainService.filterFollowing(UserContext.get(), userIds);
        List<FollowUserDTO> list = rows.stream()
                .map(row -> {
                    String uid = otherSide.apply(row);
                    return buildFollowUserDTO(uid, row.getBizId(), profileMap.get(uid), followedSet.contains(uid));
                })
                .collect(Collectors.toList());
        return new PageDTO<>(list, null, page.getHasMore());
    }

    private FollowUserDTO buildFollowUserDTO(String userId, String cursor, UserProfile userProfile, boolean isFollowed) {
        FollowUserDTO dto = new FollowUserDTO().setUserId(userId).setIsFollowed(isFollowed).setCursor(cursor);
        if (userProfile == null) {
            return dto;
        }
        UserData user = userProfile.getUser();
        if (user != null) {
            dto.setNickname(user.getNickname()).setAvatarUrl(QiniuConfiguration.buildSignedUrl(user.getAvatarUrl()));
        }
        if (userProfile.getProfile() != null) {
            dto.setNtrpScore(userProfile.getProfile().getNtrpScore());
        }
        return dto;
    }
}
