package com.rally.domain.user.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
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
        private String title;
        private ResultTypeEnum resultType; //winLose
        private MatchTypeEnum matchType;
        private String sideAPlayer1AvatarUrl;
        private String sideAPlayer2AvatarUrl;
        private String sideAScore;
        private String sideBPlayer1AvatarUrl;
        private String sideBPlayer2AvatarUrl;
        private String sideBScore;
    }

}
