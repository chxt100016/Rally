package com.rally.tournament.convert;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.tournament.model.TournamentCommentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 聊天消息到赛事评论的应用层转换。
 */
@Mapper
public interface TournamentCommentAppConvertMapper {

    TournamentCommentAppConvertMapper INSTANCE = Mappers.getMapper(TournamentCommentAppConvertMapper.class);

    @Mapping(target = "commentId", source = "bizId")
    @Mapping(target = "tournamentId", source = "refId")
    @Mapping(target = "senderAvatar", source = "senderAvatar", qualifiedByName = "parseAvatar")
    TournamentCommentDTO toCommentDTO(ChatMessageData data);

    List<TournamentCommentDTO> toCommentDTO(List<ChatMessageData> data);

    @Named("parseAvatar")
    static String parseAvatar(String key) {
        return QiniuConfiguration.buildSignedUrl(key);
    }
}
