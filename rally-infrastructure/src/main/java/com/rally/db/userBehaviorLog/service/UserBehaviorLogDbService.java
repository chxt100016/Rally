package com.rally.db.userBehaviorLog.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.userBehaviorLog.entity.UserBehaviorLogPO;
import com.rally.db.userBehaviorLog.mapper.UserBehaviorLogMapper;
import org.springframework.stereotype.Service;

@Service
public class UserBehaviorLogDbService extends ServiceImpl<UserBehaviorLogMapper, UserBehaviorLogPO> {
}
