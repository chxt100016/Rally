package com.rally.db.tournament.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tournament.entity.TournamentMatchPO;
import com.rally.db.tournament.mapper.TournamentMatchMapper;
import org.springframework.stereotype.Service;

@Service
public class TournamentMatchMybatisService extends ServiceImpl<TournamentMatchMapper, TournamentMatchPO> {
}
