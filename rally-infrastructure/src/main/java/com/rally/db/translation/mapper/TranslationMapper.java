package com.rally.db.translation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.translation.entity.TranslationPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TranslationMapper extends BaseMapper<TranslationPO> {
}
