package com.rally.db.user.repository;

import com.rally.db.user.entity.TennisProfilePO;
import com.rally.db.user.service.TennisProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TennisProfileRepository {

    private final TennisProfileService tennisProfileService;

    public TennisProfilePO save(TennisProfilePO profile) {
        return tennisProfileService.insert(profile);
    }

    public Optional<TennisProfilePO> findByUserId(String userId) {
        return tennisProfileService.findByUserId(userId);
    }

    /**
     * 批量查询网球档案（按 userId 列表）
     */
    public List<TennisProfilePO> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return tennisProfileService.lambdaQuery()
                .in(TennisProfilePO::getUserId, userIds)
                .list();
    }

    public boolean updateById(TennisProfilePO profile) {
        return tennisProfileService.updateById(profile);
    }

    public void updateScoreFields(String userId, Integer reputationScore, Integer credibilityScore,
                                  Integer calibrationScore, Boolean isNewbie) {
        tennisProfileService.lambdaUpdate()
                .eq(TennisProfilePO::getUserId, userId)
                .set(reputationScore != null, TennisProfilePO::getReputationScore, reputationScore)
                .set(credibilityScore != null, TennisProfilePO::getCredibilityScore, credibilityScore)
                .set(calibrationScore != null, TennisProfilePO::getCalibrationScore, calibrationScore)
                .set(isNewbie != null, TennisProfilePO::getIsNewbie, isNewbie)
                .update();
    }
}
