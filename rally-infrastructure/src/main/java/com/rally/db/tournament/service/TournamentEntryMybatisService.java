package com.rally.db.tournament.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tournament.entity.TournamentEntryPO;
import com.rally.db.tournament.mapper.TournamentEntryMapper;
import org.springframework.stereotype.Service;

@Service
public class TournamentEntryMybatisService extends ServiceImpl<TournamentEntryMapper, TournamentEntryPO> {
}
