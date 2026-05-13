package com.rally.client.wta.model;

import lombok.Data;

import java.util.List;

@Data
public class WtaDrawsResponse {

    private DrawData Data;

    @Data
    public static class DrawData {
        private EventInfo Event;
        /** 签表位置列表，包含球员种子和参赛类型 */
        private List<DrawEntry> Draw;
        /** 各轮次比赛结果 */
        private List<RoundResult> Results;
    }

    @Data
    public static class EventInfo {
        private Integer EventYear;
        private String EventId;
        private Integer SglDrawSize;
    }

    @Data
    public static class DrawEntry {
        private Integer DrawPosition;
        private String PlayerId;
        private String PlayerLastName;
        private String PlayerFirstName;
        private String PlayerNatlId;
        private Integer Seed;
        /** DA/BYE/WC/Q/LL 等参赛类型 */
        private String EntryType;
    }

    @Data
    public static class RoundResult {
        private RoundInfo Round;
        private List<MatchResult> Matches;
    }

    @Data
    public static class RoundInfo {
        private String Id;
        private String ShortName;
        private String LongName;
    }

    @Data
    public static class MatchResult {
        private String MatchId;
        /** F=已结束，其他状态参考 MatchStatus */
        private String MState;
        private String WinningPlayerId;
        private String Reason;
        private String PlayerId;
        private Integer PlayerSeed;
        private String PlayerLastName;
        private String PlayerFirstName;
        private String OpponentId;
        private Integer OpponentSeed;
        private String OpponentLastName;
        private String OpponentFirstName;
        private Integer Set1Player;
        private Integer Set1Opponent;
        private Integer Set1Tie;
        private Integer Set2Player;
        private Integer Set2Opponent;
        private Integer Set2Tie;
        private Integer Set3Player;
        private Integer Set3Opponent;
        private Integer Set3Tie;
        private Integer Set4Player;
        private Integer Set4Opponent;
        private Integer Set4Tie;
        private Integer Set5Player;
        private Integer Set5Opponent;
        private Integer Set5Tie;
    }
}
