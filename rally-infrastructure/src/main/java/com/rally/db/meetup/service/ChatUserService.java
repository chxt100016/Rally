package com.rally.db.meetup.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.meetup.entity.ChatUserPO;
import com.rally.db.meetup.mapper.ChatUserMapper;
import org.springframework.stereotype.Service;

/**
 * 活动群聊用户Service
 */
@Service
public class ChatUserService extends ServiceImpl<ChatUserMapper, ChatUserPO> {
}
