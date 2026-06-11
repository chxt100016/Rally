package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.meetup.entity.ChatUserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动群聊用户Mapper
 */
@Mapper
public interface ChatUserMapper extends BaseMapper<ChatUserPO> {
}
