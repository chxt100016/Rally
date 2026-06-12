package com.rally.meetup.convert;

import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.model.ChatMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 聊天App层转换器
 */
@Mapper
public interface ChatAppConvertMapper {

    ChatAppConvertMapper INSTANCE = Mappers.getMapper(ChatAppConvertMapper.class);

    /**
     * ChatMessageData -> ChatMessageDTO
     */
    @Mapping(target = "messageId", source = "bizId")
    ChatMessageDTO toChatMessageDTO(ChatMessageData data);
    List<ChatMessageDTO> toChatMessageDTO(List<ChatMessageData> data);
}
