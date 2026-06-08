package com.rally.db.user.repository;

import com.rally.db.user.entity.UserPO;
import com.rally.db.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserService userService;

    public UserPO save(UserPO user) {
        return userService.insert(user);
    }

    public boolean updateById(UserPO user) {
        return userService.updateById(user);
    }

    public Optional<UserPO> findByUserId(String userId) {
        return userService.findByUserId(userId);
    }

    /**
     * 批量查询用户（按 userId 列表）
     */
    public List<UserPO> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userService.lambdaQuery()
                .in(UserPO::getUserId, userIds)
                .list();
    }
}
