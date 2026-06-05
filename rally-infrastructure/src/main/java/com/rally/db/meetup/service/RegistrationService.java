package com.rally.db.meetup.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.mapper.RegistrationMapper;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService extends ServiceImpl<RegistrationMapper, RegistrationPO> {
}
