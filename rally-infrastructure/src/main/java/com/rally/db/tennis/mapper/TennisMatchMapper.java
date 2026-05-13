package com.rally.db.tennis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.tennis.entity.TennisMatchPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TennisMatchMapper extends BaseMapper<TennisMatchPO> {
}
