package com.rally.db.meetup.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.meetup.entity.WaitlistPO;
import com.rally.db.meetup.mapper.WaitlistMapper;
import org.springframework.stereotype.Service;

@Service
public class WaitlistService extends ServiceImpl<WaitlistMapper, WaitlistPO> {
}
