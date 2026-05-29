package com.rally.db.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.user.entity.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
