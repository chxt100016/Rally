package com.rally.db.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.score.entity.ScoreStatusPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批量评分幂等状态表 Mapper
 */
@Mapper
public interface ScoreStatusMapper extends BaseMapper<ScoreStatusPO> {
}
