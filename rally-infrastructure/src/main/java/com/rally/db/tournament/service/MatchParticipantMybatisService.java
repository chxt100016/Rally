package com.rally.db.tournament.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tournament.entity.MatchParticipantPO;
import com.rally.db.tournament.mapper.MatchParticipantMapper;
import org.springframework.stereotype.Service;

@Service
public class MatchParticipantMybatisService extends ServiceImpl<MatchParticipantMapper, MatchParticipantPO> {
}
