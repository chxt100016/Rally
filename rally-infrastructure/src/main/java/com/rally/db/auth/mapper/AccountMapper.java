package com.rally.db.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.auth.entity.AccountPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<AccountPO> {
}
