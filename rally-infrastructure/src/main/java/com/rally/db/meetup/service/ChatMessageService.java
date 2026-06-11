package com.rally.db.meetup.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.meetup.entity.ChatMessagePO;
import com.rally.db.meetup.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
 * 活动群聊消息Service
 */
@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessagePO> {
}
