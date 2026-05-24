package com.rally.client.atp.model;

import lombok.Data;

import java.util.List;

/**
 * app.atptour.com /api/v2/gateway/livematches 接口返回结构
 * 外层包装为 { Content, Data }，实际数据在 Data 字段中
 */
@Data
public class AtpAppLiveResponse {

    /** 接口外层 Data 字段，包含赛事信息和实时比赛列表 */
    private EventData Data;

    @Data
    public static class EventData {
        private Integer EventYear;
        private Integer EventId;
        private String EventTitle;
        private String EventCountryCode;
        private String EventCountry;
        private String EventLocation;
        private String EventCity;
        private String EventStartDate;
        private String EventEndDate;
        private String EventType;
        private Integer EventCurrentDayNumber;
        /** 实时比赛列表 */
        private List<LiveMatch> LiveMatches;
    }

    @Data
    public static class LiveMatch {
        private String MatchId;
        private String Org;
        private Boolean IsDoubles;
        private String RoundName;
        private String CourtName;
        private Integer CourtId;
        private String MatchTimeTotal;
        private String MatchStateReasonMessage;
        private String ExtendedMessage;
        /** 比赛状态：P=进行中, C=已完成, S=暂停 等 */
        private String MatchStatus;
        /** 发球方：1=PlayerTeam, 2=OpponentTeam */
        private Integer ServerTeam;
        private String WinningPlayerId;
        private String LastUpdated;
        /** 球员一（主队）信息，含比分 */
        private TeamInfo PlayerTeam;
        /** 球员二（客队）信息，含比分 */
        private TeamInfo OpponentTeam;
    }

    @Data
    public static class TeamInfo {
        private PlayerInfo Player;
        private PlayerInfo Partner;
        private String EntryType;
        private Integer Seed;
        /** 当前局比分，如 "0"、"15"、"30"、"40"、"AD" */
        private String GameScore;
        /** 各盘比分列表，SetNumber=0 为无效数据需过滤 */
        private List<SetScoreInfo> SetScores;
    }

    @Data
    public static class PlayerInfo {
        private String PlayerId;
        private String PlayerCountry;
        private String PlayerCountryName;
        private String PlayerFirstName;
        private String PlayerLastName;
    }

    @Data
    public static class SetScoreInfo {
        /** 盘号，0 为无效数据 */
        private Integer SetNumber;
        /** 该盘局数，Integer 类型（可为 null） */
        private Integer SetScore;
        /** 抢七分数，无抢七时为 null */
        private Integer TieBreakScore;
    }
}
