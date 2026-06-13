package com.rally.domain.user.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
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

        // 生成单条 SetItem 假数据
        public static SetItem mock(String title, ResultTypeEnum resultType, MatchTypeEnum matchType) {
            SetItem item = new SetItem();
            item.title = title;
            item.resultType = resultType;
            item.matchType = matchType;
            item.sideAPlayer1AvatarUrl = "https://example.com/avatarA1.jpg";
            item.sideAPlayer2AvatarUrl = "https://example.com/avatarA2.jpg";
            item.sideAScore = "6";
            item.sideBPlayer1AvatarUrl = "https://example.com/avatarB1.jpg";
            item.sideBPlayer2AvatarUrl = "https://example.com/avatarB2.jpg";
            item.sideBScore = "4";
            return item;
        }
    }

    // 生成整条 DTO 假数据
    public static MyProfileSetScoreDTO mock() {
        MyProfileSetScoreDTO dto = new MyProfileSetScoreDTO();
        dto.total = 50L;
        dto.singleCount = 30L;
        dto.doubleCount = 20L;
        dto.setItems = Arrays.asList(
                SetItem.mock("2024-01-15 单打", ResultTypeEnum.WIN, MatchTypeEnum.SINGLE),
                SetItem.mock("2024-01-10 双打", ResultTypeEnum.LOSE, MatchTypeEnum.DOUBLE)
        );
        return dto;
    }

}
