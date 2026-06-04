package com.rally.score.strategy;

import com.rally.domain.score.enums.ScoreDimensionEnum;
import com.rally.domain.score.model.ScoreChange;
import com.rally.domain.score.model.ScoreContext;

/**
 * 单维度评分计算策略。只负责「算」：输入上下文 + 目标用户，输出该维度的变更明细（before/after/delta）。
 * 不写库、不开事务、不记日志——这些通用能力由 ScoreManagerImpl 沉淀。
 * 便于后续新增维度（如活跃度）时只加一个策略实现，manager 编排里注册即可。
 */
public interface ScoreStrategy {

    /**
     * 该策略负责的维度，用于 manager 路由与日志 type 映射
     */
    ScoreDimensionEnum dimension();

    /**
     * 计算某用户在本维度的新分与变更明细。
     * @param ctx    本场计算上下文（meetupId、参与者、是否有比分、各只读 Gateway）
     * @param userId 目标用户
     * @return 变更明细；若该维度本次无变化返回 before==after 的 ScoreChange（manager 据此决定是否记日志）
     */
    ScoreChange calculate(ScoreContext ctx, String userId);
}
