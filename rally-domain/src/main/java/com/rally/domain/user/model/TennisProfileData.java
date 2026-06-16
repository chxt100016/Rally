package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.ProfileStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 球员档案领域数据
 */
@Data
public class TennisProfileData {
    private String bizId;
    private String userId;
    private List<VideoVO> videos;
    private BigDecimal ntrpScore;
    private BigDecimal utrScore;
    private LocalDateTime ntrpUpdatedAt;
    private ProfileStatusEnum status;
    private BigDecimal reputationScore;
    private BigDecimal credibilityScore;
    private BigDecimal calibrationScore;
    private Boolean isUnderReview;
    private Integer reviewRemainingMatches;
    private Boolean isNewbie;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 是否在核查期
     */
    public boolean underReview() {
        return Boolean.TRUE.equals(isUnderReview);
    }

    /**
     * 校验 NTRP 冷却期，冷却中则抛出业务异常
     */
    public void assertNtrpCooldown() {
        Integer remainingDays = ntrpCooldownRemainingDays();
        if (remainingDays != null) {
            throw new BusinessException(BizErrorCode.NTRP_COOLDOWN, "自评修改冷却中，" + remainingDays + " 天后可改");
        }
    }

    /**
     * 计算自评修改剩余冷却天数
     * 可编辑时返回 null，冷却中返回剩余天数
     */
    public Integer ntrpCooldownRemainingDays() {
        if (ntrpUpdatedAt == null) {
            return null;
        }
        int cooldown = resolveNtrpCooldownDays();
        long daysSince = ChronoUnit.DAYS.between(ntrpUpdatedAt, LocalDateTime.now());
        if (daysSince < cooldown) {
            return (int) (cooldown - daysSince);
        }
        return null;
    }

    /**
     * 根据可信度档位解析 NTRP 冷却总天数：可信度越高冷却越久
     */
    private int resolveNtrpCooldownDays() {
        int lowDays = SystemConfig.getInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_LOW_DAYS.getKey(), Integer.parseInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_LOW_DAYS.getDefaultValue()));
        int midDays = SystemConfig.getInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_MID_DAYS.getKey(), Integer.parseInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_MID_DAYS.getDefaultValue()));
        int highDays = SystemConfig.getInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_HIGH_DAYS.getKey(), Integer.parseInt(SystemConfigKey.SCORE_NTRP_COOLDOWN_HIGH_DAYS.getDefaultValue()));
        if (credibilityScore == null) {
            return lowDays;
        }
        float credibility = credibilityScore.floatValue();
        if (credibility < 30) {
            return lowDays;
        } else if (credibility < 60) {
            return midDays;
        } else {
            return highDays;
        }
    }

    /**
     * 校验 NTRP 涨幅是否触发核查期，触发则进入核查状态
     * @return 触发时返回 requiredMatches，未触发返回 -1
     */
    public int triggerReviewIfNeeded(BigDecimal newNtrp) {
        BigDecimal delta = ntrpScore != null ? newNtrp.subtract(ntrpScore) : BigDecimal.ZERO;
        BigDecimal triggerDelta = new BigDecimal(SystemConfig.getString(SystemConfigKey.SCORE_REVIEW_PERIOD_TRIGGER_NTRP_DELTA.getKey(), SystemConfigKey.SCORE_REVIEW_PERIOD_TRIGGER_NTRP_DELTA.getDefaultValue()));
        if (delta.compareTo(triggerDelta) < 0) {
            return -1;
        }
        int requiredMatches = SystemConfig.getInt(SystemConfigKey.SCORE_REVIEW_PERIOD_REQUIRED_MATCHES.getKey(), Integer.parseInt(SystemConfigKey.SCORE_REVIEW_PERIOD_REQUIRED_MATCHES.getDefaultValue()));
        this.status = ProfileStatusEnum.UNDER_REVIEW;
        this.isUnderReview = true;
        this.reviewRemainingMatches = requiredMatches;
        return requiredMatches;
    }

    /**
     * 更新 NTRP 分值并刷新更新时间（冷却期起点）
     */
    public void updateNtrpScore(BigDecimal newNtrp) {
        this.ntrpScore = newNtrp;
        this.ntrpUpdatedAt = LocalDateTime.now();
    }

    /**
     * 初始化为 TBC（待完善）档案
     */
    public void initTBC(String userId) {
        this.userId = userId;
        this.status = ProfileStatusEnum.TBC;
        this.videos = new ArrayList<>();
    }

    /**
     * 完成 onboarding 的档案部分：落 NTRP 自评和视频，状态置为 NORMAL
     */
    public void completeOnboarding(BigDecimal ntrpScore, List<VideoVO> videos) {
        this.ntrpScore = ntrpScore;
        this.videos = videos;
        this.status = ProfileStatusEnum.NORMAL;
        this.ntrpUpdatedAt = LocalDateTime.now();
    }

    /**
     * 追加一条视频
     */
    public void addVideo(VideoVO video) {
        if (this.videos == null) {
            this.videos = new ArrayList<>();
        }
        this.videos.add(video);
    }

    /**
     * 删除一条视频
     */
    public void deleteVideo(String key) {
        if (this.videos == null) {
            return;
        }
        this.videos.removeIf(v -> v.getKey().equals(key));
    }

    /**
     * 修改视频标题
     */
    public void updateVideo(String key, String title) {
        if (this.videos == null) {
            return;
        }
        this.videos.stream()
                .filter(v -> v.getKey().equals(key))
                .findFirst()
                .ifPresent(v -> v.setTitle(title));
    }
}
