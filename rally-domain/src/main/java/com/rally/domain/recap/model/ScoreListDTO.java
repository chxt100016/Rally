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

    private Long total;
    private Long singleCount;
    private Long doubleCount;
    private String winRate;
    private String singleWinRate;
    private String doubleWinRate;
    private String streakType;
    private Long streakCount;
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
        private String sideAPlayer1Id;
        private String sideAPlayer1Nickname;
        private String sideAPlayer1AvatarUrl;
        private String sideAPlayer2Id;
        private String sideAPlayer2Nickname;
        private String sideAPlayer2AvatarUrl;
        private String sideAScore;
        private String sideBPlayer1Id;
        private String sideBPlayer1Nickname;
        private String sideBPlayer1AvatarUrl;
        private String sideBPlayer2Id;
        private String sideBPlayer2Nickname;
        private String sideBPlayer2AvatarUrl;
        private String sideBScore;
    }
}
