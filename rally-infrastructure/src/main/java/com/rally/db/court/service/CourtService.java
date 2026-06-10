package com.rally.db.court.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.mapper.CourtMapper;
import org.springframework.stereotype.Service;

@Service
public class CourtService extends ServiceImpl<CourtMapper, CourtPO> {
}
