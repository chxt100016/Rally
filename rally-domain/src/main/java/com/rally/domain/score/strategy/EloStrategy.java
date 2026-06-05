package com.rally.domain.score.strategy;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.review.gateway.ScoreRecordGateway;
import com.rally.domain.review.model.ScoreRecordData;
import com.rally.domain.score.gateway.PlayerEloGateway;
import com.rally.domain.score.model.EloResult;
import com.rally.domain.score.model.ScoreContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ELO 策略（仅有比分时触发）
 * 标准公式 + 双打队均分 + 比分悬殊/接近系数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EloStrategy {

    private final ScoreRecordGateway scoreRecordGateway;
    private final PlayerEloGateway playerEloGateway;

    /**
     * 计算一场约球的 ELO 变更（多盘累计）
     * @return 每个用户的 ELO 变更结果
     */
    public List<EloResult> calculateMatch(ScoreContext ctx) {
        // 1. 获取本场全部比分盘（按 set_number 升序）
        List<ScoreRecordData> sets = scoreRecordGateway.listByMeetupId(ctx.getMeetupId());
        if (sets == null || sets.isEmpty()) {
            return List.of();
        }

        // 2. 收集本场涉及的所有 user_id
        Set<String> allUserIds = new HashSet<>();
        for (ScoreRecordData set : sets) {
            allUserIds.add(set.getSideAPlayer1());
            if (set.getSideAPlayer2() != null) {
                allUserIds.add(set.getSideAPlayer2());
            }
            allUserIds.add(set.getSideBPlayer1());
            if (set.getSideBPlayer2() != null) {
                allUserIds.add(set.getSideBPlayer2());
            }
        }

        // 3. 从 player_elo 读当前分（无则按 initial 初始化）
        Map<String, Float> currentElo = new HashMap<>();
        float initialElo = SystemConfig.getFloat("score.elo.initial", 1500);
        for (String userId : allUserIds) {
            currentElo.put(userId, playerEloGateway.getEloScore(userId));
        }

        // 4. 初始化 EloResult（记录 before）
        Map<String, EloResult> results = new HashMap<>();
        for (String userId : allUserIds) {
            results.put(userId, new EloResult(userId, currentElo.get(userId)));
        }

        // 5. 逐盘计算 ELO 变更
        float kFactor = SystemConfig.getFloat("score.elo.k_factor", 32);
        float blowoutOffset = SystemConfig.getFloat("score.elo.blowout_offset", 0.1f);
        float closeOffset = SystemConfig.getFloat("score.elo.close_offset", 0.05f);

        for (ScoreRecordData set : sets) {
            // A 侧选手
            List<String> sideA = new ArrayList<>();
            sideA.add(set.getSideAPlayer1());
            if (set.getSideAPlayer2() != null) {
                sideA.add(set.getSideAPlayer2());
            }

            // B 侧选手
            List<String> sideB = new ArrayList<>();
            sideB.add(set.getSideBPlayer1());
            if (set.getSideBPlayer2() != null) {
                sideB.add(set.getSideBPlayer2());
            }

            // 计算队均分
            float avgA = calculateTeamAvg(sideA, currentElo);
            float avgB = calculateTeamAvg(sideB, currentElo);

            // 预期胜率
            float eA = 1.0f / (1.0f + (float) Math.pow(10, (avgB - avgA) / 400.0));
            float eB = 1.0f - eA;

            // 判定胜负
            int scoreA = set.getSideAScore();
            int scoreB = set.getSideBScore();
            boolean aWins = scoreA > scoreB;

            // 基础 S 值
            float sA = aWins ? 1.0f : 0.0f;
            float sB = aWins ? 0.0f : 1.0f;

            // 比分悬殊/接近系数
            float[] offsets = calculateScoreOffset(scoreA, scoreB, blowoutOffset, closeOffset);
            sA += offsets[0];
            sB += offsets[1];

            // 计算 delta
            float deltaA = kFactor * (sA - eA);
            float deltaB = kFactor * (sB - eB);

            // 应用 delta（同队同 delta）
            for (String userId : sideA) {
                results.get(userId).applyDelta(deltaA);
                currentElo.put(userId, currentElo.get(userId) + deltaA);
            }
            for (String userId : sideB) {
                results.get(userId).applyDelta(deltaB);
                currentElo.put(userId, currentElo.get(userId) + deltaB);
            }
        }

        // 6. 设置 match_count
        for (EloResult result : results.values()) {
            result.setMatchCount(sets.size());
        }

        return new ArrayList<>(results.values());
    }

    /**
     * 计算队均分
     */
    private float calculateTeamAvg(List<String> team, Map<String, Float> eloMap) {
        float sum = 0;
        for (String userId : team) {
            sum += eloMap.getOrDefault(userId, 1500f);
        }
        return sum / team.size();
    }

    /**
     * 计算比分悬殊/接近系数偏移
     * @return [sideA_offset, sideB_offset]
     */
    private float[] calculateScoreOffset(int scoreA, int scoreB, float blowoutOffset, float closeOffset) {
        int diff = Math.abs(scoreA - scoreB);
        int loserScore = Math.min(scoreA, scoreB);

        // 悬殊：胜负局差 ≥5 或负方 ≤1
        if (diff >= 5 || loserScore <= 1) {
            return scoreA > scoreB
                    ? new float[]{blowoutOffset, -blowoutOffset}
                    : new float[]{-blowoutOffset, blowoutOffset};
        }

        // 接近：负方 score ∈ {5,6} 且差 ≤2（即 7-5/7-6）
        if ((loserScore == 5 || loserScore == 6) && diff <= 2) {
            return scoreA > scoreB
                    ? new float[]{-closeOffset, closeOffset}
                    : new float[]{closeOffset, -closeOffset};
        }

        // 标准：无偏移
        return new float[]{0, 0};
    }
}
