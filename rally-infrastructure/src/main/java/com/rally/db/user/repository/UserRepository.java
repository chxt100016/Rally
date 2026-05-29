package com.rally.db.user.repository;

import com.rally.db.user.entity.UserPO;
import com.rally.db.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
