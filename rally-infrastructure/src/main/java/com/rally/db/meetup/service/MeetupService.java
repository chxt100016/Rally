package com.rally.db.meetup.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.mapper.MeetupMapper;
import org.springframework.stereotype.Service;

@Service
public class MeetupService extends ServiceImpl<MeetupMapper, MeetupPO> {
}
