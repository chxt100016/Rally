package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.meetup.entity.WaitlistPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WaitlistMapper extends BaseMapper<WaitlistPO> {
}
