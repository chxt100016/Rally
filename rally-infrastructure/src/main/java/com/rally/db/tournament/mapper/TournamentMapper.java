package com.rally.db.tournament.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.tournament.entity.TournamentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TournamentMapper extends BaseMapper<TournamentPO> {
}
