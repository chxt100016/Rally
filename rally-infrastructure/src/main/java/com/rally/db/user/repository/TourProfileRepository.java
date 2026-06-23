package com.rally.db.user.repository;

import com.rally.db.user.entity.TourProfilePO;
import com.rally.db.user.service.TourProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TourProfileRepository {

    private final TourProfileService tourProfileService;

    public TourProfilePO save(TourProfilePO profile) {
        return tourProfileService.insert(profile);
    }

    public Optional<TourProfilePO> findByUserId(String userId) {
        return tourProfileService.findByUserId(userId);
    }

    /**
     * 批量查询网球档案（按 userId 列表）
     */
    public List<TourProfilePO> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return tourProfileService.lambdaQuery()
                .in(TourProfilePO::getUserId, userIds)
                .list();
    }

    public boolean updateById(TourProfilePO profile) {
        return tourProfileService.updateById(profile);
    }

    public void updateScoreFields(String userId, Integer reputationScore, Integer credibilityScore,
                                  Integer calibrationScore, Boolean isNewbie) {
        tourProfileService.lambdaUpdate()
                .eq(TourProfilePO::getUserId, userId)
                .set(reputationScore != null, TourProfilePO::getReputationScore, reputationScore)
                .set(credibilityScore != null, TourProfilePO::getCredibilityScore, credibilityScore)
                .set(calibrationScore != null, TourProfilePO::getCalibrationScore, calibrationScore)
                .set(isNewbie != null, TourProfilePO::getIsNewbie, isNewbie)
                .update();
    }
}
