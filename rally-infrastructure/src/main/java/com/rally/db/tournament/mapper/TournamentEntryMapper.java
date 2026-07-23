package com.rally.db.tournament.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.tournament.entity.TournamentEntryPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TournamentEntryMapper extends BaseMapper<TournamentEntryPO> {
}
