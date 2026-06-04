package com.rally.db.score.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.score.entity.ScoreStatusPO;
import com.rally.db.score.mapper.ScoreStatusMapper;
import org.springframework.stereotype.Service;

/**
 * 批量评分幂等状态表 Service
 */
@Service
public class ScoreStatusService extends ServiceImpl<ScoreStatusMapper, ScoreStatusPO> {
}
