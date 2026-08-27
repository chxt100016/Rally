package com.rally.socialrelations.followerslist.activity;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.user.model.FollowListCmd;
import com.rally.domain.user.model.FollowUserDTO;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserFollowData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserFollowDomainService;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务活动 query-followers-page：游标分页查询粉丝并补充公开档案。
 */
@Component
@RequiredArgsConstructor
public class QueryFollowersPageActivity {

    private final UserFollowDomainService userFollowDomainService;
    private final UserProfileDomainService userProfileDomainService;

    public PageDTO<FollowUserDTO> execute(String currentUserId, FollowListCmd command) {
        // A1-A2：指定编号不做存在性检查；关系查询保留 main 的字符串游标、
        // bizId 倒序与 size+1 裁剪语义。
        String listOwnerId = StringUtils.isNotBlank(command.getUserId())
                ? command.getUserId()
                : currentUserId;
        PageDTO<UserFollowData> page = userFollowDomainService.listFollowers(listOwnerId, command);

        List<UserFollowData> rows = page.getList();
        if (rows.isEmpty()) {
            return new PageDTO<>(List.of(), null, page.getHasMore());
        }

        // A3：关系不因用户或网球档案缺失而被过滤。
        List<String> followerIds = rows.stream()
                .map(UserFollowData::getFollowerId)
                .toList();
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(followerIds);

        // A4：此集合以当前登录用户为 follower，不使用名单所属用户。
        Set<String> followedSet = userFollowDomainService.filterFollowing(currentUserId, followerIds);
        List<FollowUserDTO> list = rows.stream()
                .map(row -> buildItem(
                        row.getFollowerId(),
                        row.getBizId(),
                        profileMap.get(row.getFollowerId()),
                        followedSet.contains(row.getFollowerId())))
                .toList();
        return new PageDTO<>(list, null, page.getHasMore());
    }

    private FollowUserDTO buildItem(
            String userId,
            String cursor,
            UserProfile profile,
            boolean followed) {
        FollowUserDTO item = new FollowUserDTO()
                .setUserId(userId)
                .setCursor(cursor)
                .setIsFollowed(followed);
        if (profile == null) {
            return item;
        }
        UserData user = profile.getUser();
        if (user != null) {
            item.setNickname(user.getNickname())
                    .setAvatarUrl(QiniuConfiguration.buildSignedUrl(user.getAvatarUrl()));
        }
        if (profile.getProfile() != null) {
            item.setNtrpScore(profile.getProfile().getNtrpScore());
        }
        return item;
    }
}
