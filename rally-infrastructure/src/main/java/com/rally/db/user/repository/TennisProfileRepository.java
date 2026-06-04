package com.rally.db.user.repository;

import com.rally.db.user.entity.TennisProfilePO;
import com.rally.db.user.service.TennisProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    public boolean updateById(TennisProfilePO profile) {
        return tennisProfileService.updateById(profile);
    }

    public void updateScoreFields(String userId, BigDecimal reputationScore, BigDecimal credibilityScore,
                                  BigDecimal calibrationScore, Boolean isNewbie) {
        tennisProfileService.lambdaUpdate()
                .eq(TennisProfilePO::getUserId, userId)
                .set(reputationScore != null, TennisProfilePO::getReputationScore, reputationScore)
                .set(credibilityScore != null, TennisProfilePO::getCredibilityScore, credibilityScore)
                .set(calibrationScore != null, TennisProfilePO::getCalibrationScore, calibrationScore)
                .set(isNewbie != null, TennisProfilePO::getIsNewbie, isNewbie)
                .update();
    }
}
