package com.rally.db.translation.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.translation.entity.TranslationPO;
import com.rally.db.translation.mapper.TranslationMapper;
import org.springframework.stereotype.Service;

@Service
public class TranslationMybatisService extends ServiceImpl<TranslationMapper, TranslationPO> {
}
