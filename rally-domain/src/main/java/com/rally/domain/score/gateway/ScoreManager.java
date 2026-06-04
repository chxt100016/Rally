package com.rally.domain.score.gateway;

import com.rally.domain.score.model.ReputationPenaltyCmd;

/**
 * 评分域对外唯一入口。实现 ScoreManagerImpl 标注 @Transactional，是评分写入的事务入口：
 * 编排各策略计算 → 写 user_tennis_profile 三维分/总分/等级 → 记 user_profile_change_log
 * → 推进/解除核查期 → 更新 player_elo → 维护 rally_meetup_score_status。
 * 任一步异常整体回滚。
 */
public interface ScoreManager {

    /**
     * 全量重算一场约球涉及的所有参与者：信誉/可信/校准三维 + 总分 + 等级；
     * 若该场存在比分记录则同时更新 ELO。
     * 幂等：以 meetupId 为键，可重复调用（实时触发与凌晨批量共用此入口）。
     * 调用方（03）在自己的 @Transactional 写方法内调用，本方法继承该事务。
     * @param meetupId 约球 biz_id
     */
    void recalc(String meetupId);

    /**
     * 单点信誉分扣减（约球域 02 触发：退出 <6h、发布者关闭阶梯）。
     * 仅改信誉分 → 重算总分/等级 → 记一条 reputation 变更日志。不触碰可信/校准/ELO。
     * 同样是事务入口，02 在释放名额的同事务内调用。
     * @param cmd userId + reason(原因码) + refMeetupId
     */
    void applyReputationPenalty(ReputationPenaltyCmd cmd);
}
