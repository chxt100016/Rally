package com.rally.domain.recap.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;
import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScoreItemDTO {
    private String bizId;
    private String meetupId;
    private ResultTypeEnum resultType;
    private String resultTypeShow;
    private MatchTypeEnum matchType;
    private String matchTypeShow;
    private SetFormatEnum setFormat;
    private String setFormatShow;
    private String date;
    private String myScore;
    private String opponentScore;
    private GenderEnum myGender;
    private String teammateId;
    private String teammateNickname;
    private String teammateAvatarUrl;
    private GenderEnum teammateGender;
    private String opponent1Id;
    private String opponent1Nickname;
    private String opponent1AvatarUrl;
    private GenderEnum opponent1Gender;
    private String opponent2Id;
    private String opponent2Nickname;
    private String opponent2AvatarUrl;
    private GenderEnum opponent2Gender;
}
