package com.rally.domain.user.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MyProfileSetScoreDTO {

    private Long total;

    private Long singleCount;

    private Long doubleCount;

    private List<SetItem> setItems;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class SetItem{
        private ResultTypeEnum resultType;
        private String resultTypeShow;
        private MatchTypeEnum matchType;
        private String matchTypeShow;
        private SetFormatEnum setFormat;
        private String setFormatShow;
        private String date;
        private String sideAPlayer1UserId;
        private String sideAPlayer1AvatarUrl;
        private String sideAPlayer2UserId;
        private String sideAPlayer2AvatarUrl;
        private String sideAScore;
        private String sideBPlayer1UserId;
        private String sideBPlayer1AvatarUrl;
        private String sideBPlayer2UserId;
        private String sideBPlayer2AvatarUrl;
        private String sideBScore;
    }

}
