package com.rally.db.tournament.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tournament.entity.TournamentPO;
import com.rally.db.tournament.mapper.TournamentMapper;
import org.springframework.stereotype.Service;

@Service
public class TournamentService extends ServiceImpl<TournamentMapper, TournamentPO> {
}
