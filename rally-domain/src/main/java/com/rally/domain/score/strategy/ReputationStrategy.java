package com.rally.domain.score.strategy;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.recap.enums.AttendanceEnum;
import com.rally.domain.recap.gateway.ReviewGateway;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.score.enums.ScoreDimensionEnum;
import com.rally.domain.score.model.ScoreChange;
import com.rally.domain.score.model.ScoreContext;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 信誉分策略（行为驱动增量分）
 * 默认 100，按行为加减，下限 0（不为负）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReputationStrategy implements ScoreStrategy {

    private final ReviewGateway reviewGateway;
    private final TennisProfileGateway profileGateway;

    @Override
    public ScoreDimensionEnum dimension() {
        return ScoreDimensionEnum.REPUTATION;
    }

    @Override
    public ScoreChange calculate(ScoreContext ctx, String userId) {
        ScoreChange change = new ScoreChange();
        change.setDimension(ScoreDimensionEnum.REPUTATION);
        change.setRefId(ctx.getMeetupId());

        // 获取当前信誉分
        BigDecimal before = profileGateway.findByUserId(userId)
                .map(TennisProfileData::getReputationScore)
                .orElse(BigDecimal.valueOf(100));
        change.setBefore(before);

        // 查本场所有对该用户的出勤评价
        List<ReviewData> attendanceReviews = reviewGateway.findByDimension(
                ctx.getMeetupId(), null, userId, "attendance");

        // 判定本场出勤结论（最严优先：任一 no_show → no_show；否则任一 late → late；否则 on_time）
        String verdict = determineVerdict(attendanceReviews);

        // 按结论读配置扣分值
        int delta = resolveDelta(verdict);

        // 计算新分（下限 0，上限 100）
        BigDecimal after = before.add(BigDecimal.valueOf(delta));
        after = after.max(BigDecimal.ZERO);
        after = after.min(BigDecimal.valueOf(100));

        change.setAfter(after);
        change.setValue(after.subtract(before));
        change.setReason(resolveReason(verdict));
        change.setRemark("出勤评价: " + verdict);

        return change;
    }

    /**
     * 判定出勤结论（最严优先）
     */
    private String determineVerdict(List<ReviewData> reviews) {
        boolean hasNoShow = false;
        boolean hasLate = false;

        for (ReviewData review : reviews) {
            String value = review.getReviewValue();
            if (AttendanceEnum.NO_SHOW.name().equalsIgnoreCase(value)) {
                hasNoShow = true;
                break; // 爽约不可洗白
            }
            if (AttendanceEnum.LATE.name().equalsIgnoreCase(value)) {
                hasLate = true;
            }
        }

        if (hasNoShow) {
            return "no_show";
        }
        if (hasLate) {
            return "late";
        }
        return "on_time";
    }

    /**
     * 按出勤结论读配置扣分值
     */
    private int resolveDelta(String verdict) {
        return switch (verdict) {
            case "no_show" -> SystemConfig.getInt("score.reputation.no_show", -25);
            case "late" -> SystemConfig.getInt("score.reputation.late", -10);
            default -> SystemConfig.getInt("score.reputation.on_time", 2);
        };
    }

    /**
     * 映射出勤结论到变更原因枚举
     */
    private ChangeReasonEnum resolveReason(String verdict) {
        return switch (verdict) {
            case "no_show" -> ChangeReasonEnum.NO_SHOW;
            case "late" -> ChangeReasonEnum.LATE;
            default -> ChangeReasonEnum.ON_TIME;
        };
    }
}
