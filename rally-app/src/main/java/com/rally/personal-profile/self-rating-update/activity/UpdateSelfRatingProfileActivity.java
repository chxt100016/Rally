package com.rally.personalprofile.selfratingupdate.activity;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.gateway.TennisProfileRepository;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务活动 update-self-rating-profile：校验冷却并更新本人 NTRP 档案。
 */
@Component
@RequiredArgsConstructor
public class UpdateSelfRatingProfileActivity {

    private final UserProfileDomainService userProfileDomainService;
    private final TennisProfileRepository tennisProfileRepository;

    public SelfRatingUpdateContext execute(String userId, BigDecimal newNtrp) {
        // A1：基础用户不存在仍报 TOKEN_INVALID；无网球档案时保留 main 的自然失败语义。
        UserProfile userProfile = userProfileDomainService.get(userId);
        TennisProfileData profile = userProfile.getProfile();
        BigDecimal oldNtrp = profile.getNtrpScore();
        int cooldownDays = resolveCooldownDays(profile.getCredibilityScore());

        // A2-A3：C9 按 ChronoUnit.DAYS 校验冷却，先计算涨幅并可选进入核查期，再刷新 NTRP 与时间。
        BigDecimal delta = oldNtrp == null ? BigDecimal.ZERO : newNtrp.subtract(oldNtrp);
        BigDecimal triggerDelta = SystemConfig.getBigDecimal(
                SystemConfigKey.SCORE_REVIEW_PERIOD_TRIGGER_NTRP_DELTA.getKey());
        int configuredRequiredMatches = SystemConfig.getInt(
                SystemConfigKey.SCORE_REVIEW_PERIOD_REQUIRED_MATCHES.getKey());
        int requiredMatches = userProfile.changeSelfRatedNtrp(
                newNtrp,
                LocalDateTime.now(),
                cooldownDays,
                triggerDelta,
                configuredRequiredMatches);

        // A4：使用既有部分字段更新，故新设 reviewRemainingMatches 仍不持久化。
        tennisProfileRepository.update(profile);
        return new SelfRatingUpdateContext(userId, oldNtrp, newNtrp, delta, requiredMatches);
    }

    private int resolveCooldownDays(Integer credibilityScore) {
        if (credibilityScore == null || credibilityScore < 30) {
            return SystemConfig.getInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_LOW_DAYS.getKey());
        }
        if (credibilityScore < 60) {
            return SystemConfig.getInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_MID_DAYS.getKey());
        }
        return SystemConfig.getInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_HIGH_DAYS.getKey());
    }
}
