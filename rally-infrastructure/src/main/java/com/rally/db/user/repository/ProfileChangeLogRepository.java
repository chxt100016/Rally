package com.rally.db.user.repository;

import com.rally.db.user.entity.ProfileChangeLogPO;
import com.rally.db.user.service.ProfileChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProfileChangeLogRepository {

    private final ProfileChangeLogService profileChangeLogService;

    public ProfileChangeLogPO save(ProfileChangeLogPO log) {
        return profileChangeLogService.insert(log);
    }

    public List<ProfileChangeLogPO> findByUserIdAndType(String userId, String type) {
        return profileChangeLogService.findByUserIdAndType(userId, type);
    }

    public Optional<ProfileChangeLogPO> findLatestUnderReviewLog(String userId) {
        return profileChangeLogService.findLatestUnderReviewLog(userId);
    }
}
