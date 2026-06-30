package com.rally.domain.recap.convert;

import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface ScoreConvertMapper {

    ScoreConvertMapper INSTANCE = Mappers.getMapper(ScoreConvertMapper.class);

    @Mapping(target = "bizId", source = "bizId")
    @Mapping(target = "rallyMeetupId", source = "meetupId")
    @Mapping(target = "setNumber", source = "cmd.setNum")
    @Mapping(target = "setFormat", source = "cmd.setFormatType")
    @Mapping(target = "recordedBy", source = "userId")
    @Mapping(target = "meetupDate", source = "meetupDate")
    @Mapping(target = "venueName", source = "venueName")
    @Mapping(target = "winSide", ignore = true)
    @Mapping(target = "sideAPlayer1Nickname", ignore = true)
    @Mapping(target = "sideAPlayer1Avatar", ignore = true)
    @Mapping(target = "sideAPlayer2Nickname", ignore = true)
    @Mapping(target = "sideAPlayer2Avatar", ignore = true)
    @Mapping(target = "sideBPlayer1Nickname", ignore = true)
    @Mapping(target = "sideBPlayer1Avatar", ignore = true)
    @Mapping(target = "sideBPlayer2Nickname", ignore = true)
    @Mapping(target = "sideBPlayer2Avatar", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ScoreRecordData toScoreRecordData(ScoreAddCmd cmd, String bizId, String meetupId, String userId, LocalDateTime meetupDate, String venueName);

    @Mapping(target = "rallyMeetupId", source = "meetupId")
    @Mapping(target = "setNumber", source = "cmd.setNum")
    @Mapping(target = "setFormat", source = "cmd.setFormatType")
    @Mapping(target = "recordedBy", source = "userId")
    @Mapping(target = "meetupDate", source = "meetupDate")
    @Mapping(target = "venueName", source = "venueName")
    @Mapping(target = "winSide", ignore = true)
    @Mapping(target = "sideAPlayer1Nickname", ignore = true)
    @Mapping(target = "sideAPlayer1Avatar", ignore = true)
    @Mapping(target = "sideAPlayer2Nickname", ignore = true)
    @Mapping(target = "sideAPlayer2Avatar", ignore = true)
    @Mapping(target = "sideBPlayer1Nickname", ignore = true)
    @Mapping(target = "sideBPlayer1Avatar", ignore = true)
    @Mapping(target = "sideBPlayer2Nickname", ignore = true)
    @Mapping(target = "sideBPlayer2Avatar", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ScoreRecordData toScoreRecordData(ScoreUpdateCmd cmd, String meetupId, String userId, LocalDateTime meetupDate, String venueName);

    default void fillUserinfo(ScoreRecordData data, Map<String, UserProfile> profileMap) {
        if (profileMap == null || profileMap.isEmpty()) {
            return;
        }
        UserProfile profileA1 = profileMap.get(data.getSideAPlayer1());
        if (profileA1 != null && profileA1.getUser() != null) {
            UserData a1 = profileA1.getUser();
            data.setSideAPlayer1Nickname(a1.getNickname());
            data.setSideAPlayer1Avatar(a1.getAvatarUrl());
        }
        UserProfile profileA2 = profileMap.get(data.getSideAPlayer2());
        if (profileA2 != null && profileA2.getUser() != null) {
            UserData a2 = profileA2.getUser();
            data.setSideAPlayer2Nickname(a2.getNickname());
            data.setSideAPlayer2Avatar(a2.getAvatarUrl());
        }
        UserProfile profileB1 = profileMap.get(data.getSideBPlayer1());
        if (profileB1 != null && profileB1.getUser() != null) {
            UserData b1 = profileB1.getUser();
            data.setSideBPlayer1Nickname(b1.getNickname());
            data.setSideBPlayer1Avatar(b1.getAvatarUrl());
        }
        UserProfile profileB2 = profileMap.get(data.getSideBPlayer2());
        if (profileB2 != null && profileB2.getUser() != null) {
            UserData b2 = profileB2.getUser();
            data.setSideBPlayer2Nickname(b2.getNickname());
            data.setSideBPlayer2Avatar(b2.getAvatarUrl());
        }
    }
}
