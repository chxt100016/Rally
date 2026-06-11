package com.rally.db.meetup.convert;

import com.rally.db.meetup.entity.ChatMessagePO;
import com.rally.db.meetup.entity.ChatUserPO;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.model.ChatUserData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 聊天PO与Data转换器
 */
@Mapper
public interface ChatConvertMapper {

    ChatConvertMapper INSTANCE = Mappers.getMapper(ChatConvertMapper.class);

    /**
     * ChatMessageData -> ChatMessagePO
     */
    ChatMessagePO toChatMessagePO(ChatMessageData data);

    /**
     * ChatMessagePO -> ChatMessageData
     */
    ChatMessageData toChatMessageData(ChatMessagePO po);

    /**
     * ChatUserData -> ChatUserPO
     */
    ChatUserPO toChatUserPO(ChatUserData data);

    /**
     * ChatUserPO -> ChatUserData
     */
    ChatUserData toChatUserData(ChatUserPO po);
}
