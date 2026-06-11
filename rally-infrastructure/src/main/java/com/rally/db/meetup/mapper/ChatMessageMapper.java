package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.meetup.entity.ChatMessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动群聊消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessagePO> {
}
