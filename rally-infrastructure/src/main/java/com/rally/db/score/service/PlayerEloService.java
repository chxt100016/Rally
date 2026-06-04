package com.rally.db.score.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.score.entity.PlayerEloPO;
import com.rally.db.score.mapper.PlayerEloMapper;
import org.springframework.stereotype.Service;

/**
 * ELO 聚合表 Service
 */
@Service
public class PlayerEloService extends ServiceImpl<PlayerEloMapper, PlayerEloPO> {
}
