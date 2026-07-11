package com.rally.db.userExt.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.userExt.entity.UserExtPO;
import com.rally.db.userExt.mapper.UserExtMapper;
import org.springframework.stereotype.Service;

@Service
public class UserExtService extends ServiceImpl<UserExtMapper, UserExtPO> {
}
