package com.rally.domain.recap.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ScoreListDTO {

    private List<Item> list;
    private Boolean hasMore;
    private String nextCursor;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private String bizId;
        private ResultTypeEnum resultType;
        private String resultTypeShow;
        private MatchTypeEnum matchType;
        private String matchTypeShow;
        private SetFormatEnum setFormat;
        private String setFormatShow;
        private String date;
        private String myScore;
        private String opponentScore;
        private String opponent1Id;
        private String opponent1Nickname;
        private String opponent1AvatarUrl;
        private String opponent2Id;
        private String opponent2Nickname;
        private String opponent2AvatarUrl;
    }
}
