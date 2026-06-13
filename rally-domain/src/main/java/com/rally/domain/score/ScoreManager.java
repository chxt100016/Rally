package com.rally.domain.score;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 评分管理器实现（事务入口）
 * 编排策略计算 → 写 profile → 记 change_log → 推进核查期 → 维护 status
 */
@Slf4j
@Service
public class ScoreManager {



    public void recalc(String meetupId) {
        //TODO
    }




}
