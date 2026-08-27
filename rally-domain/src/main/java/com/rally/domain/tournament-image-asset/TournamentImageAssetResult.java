package com.rally.domain.content.imageasset;

/**
 * 赛事主图与背景图生成结果。键只在对应对象保存成功后返回。
 */
public final class TournamentImageAssetResult {

    private final TournamentImageAssetOutcome outcome;
    private final String imageKey;
    private final String backgroundKey;

    private TournamentImageAssetResult(TournamentImageAssetOutcome outcome,
                                       String imageKey,
                                       String backgroundKey) {
        this.outcome = outcome;
        this.imageKey = imageKey;
        this.backgroundKey = backgroundKey;
    }

    public static TournamentImageAssetResult failedBeforeSave() {
        return new TournamentImageAssetResult(
                TournamentImageAssetOutcome.FAILED_BEFORE_SAVE, null, null);
    }

    public static TournamentImageAssetResult failedAfterMainSave(String imageKey) {
        return new TournamentImageAssetResult(
                TournamentImageAssetOutcome.FAILED_AFTER_MAIN_SAVE, imageKey, null);
    }

    public static TournamentImageAssetResult success(String imageKey, String backgroundKey) {
        return new TournamentImageAssetResult(
                TournamentImageAssetOutcome.SUCCESS, imageKey, backgroundKey);
    }

    public TournamentImageAssetOutcome getOutcome() {
        return outcome;
    }

    public String getImageKey() {
        return imageKey;
    }

    public String getBackgroundKey() {
        return backgroundKey;
    }
}
