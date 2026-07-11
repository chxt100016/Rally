package com.rally.domain.user.gateway;

import com.rally.domain.user.model.UserExtData;

import java.util.List;
import java.util.Optional;

public interface UserExtRepository {
    void save(UserExtData data);
    Optional<UserExtData> findByUserIdAndKey(String userId, String extKey);
    List<UserExtData> findAllByUserId(String userId);
    void deleteByUserIdAndKey(String userId, String extKey);
}
