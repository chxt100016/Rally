package com.rally.db.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.score.entity.PlayerEloPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ELO 聚合表 Mapper
 */
@Mapper
public interface PlayerEloMapper extends BaseMapper<PlayerEloPO> {
}
