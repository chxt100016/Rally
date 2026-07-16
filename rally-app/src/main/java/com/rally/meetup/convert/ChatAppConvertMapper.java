package com.rally.meetup.convert;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.model.ChatMessageDTO;
import com.rally.domain.meetup.model.ChatUnreadUserData;
import com.rally.domain.meetup.model.ChatUnreadUserDTO;
import com.rally.domain.user.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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
    @Mapping(target = "senderAvatar", source = "senderAvatar", qualifiedByName = "parseAvatar")
    ChatMessageDTO toChatMessageDTO(ChatMessageData data);
    List<ChatMessageDTO> toChatMessageDTO(List<ChatMessageData> data);

    /**
     * ChatUnreadUserData + UserProfile -> ChatUnreadUserDTO（profile 为空则昵称/头像为空）
     */
    @Mapping(target = "userId", source = "data.userId")
    @Mapping(target = "lastReadTime", source = "data.lastReadTime")
    @Mapping(target = "nickname", source = "profile.user.nickname")
    @Mapping(target = "avatarUrl", source = "profile.user.avatarUrl", qualifiedByName = "parseAvatar")
    ChatUnreadUserDTO toChatUnreadUserDTO(ChatUnreadUserData data, UserProfile profile);

    @Named("parseAvatar")
    static String parseAvatar(String key) {
        return QiniuConfiguration.buildSignedUrl(key);
    }
}
