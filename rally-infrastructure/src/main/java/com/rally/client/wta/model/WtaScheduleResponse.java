package com.rally.client.wta.model;

import lombok.Data;

import java.util.List;

@Data
public class WtaScheduleResponse {

    private Object Content;
    private ScheduleData Data;

    @Data
    public static class ScheduleData {
        private Integer EventYear;
        private Integer EventId;
        private String EventType;
        private String SchedulePdfUrl;
        private List<String> MatchDates;
        private String ActiveScheduleDay;
        private List<ScheduleDay> ScheduleDays;
    }

    @Data
    public static class ScheduleDay {
        private String MatchDate;
        private Integer DaySequence;
        private Boolean IsActiveDay;
        private List<ScheduleCourt> ScheduleCourts;
    }

    @Data
    public static class ScheduleCourt {
        private Integer CourtId;
        private String CourtName;
        private String CityName;
        private List<ScheduleMatch> ScheduleMatches;
    }

    @Data
    public static class ScheduleMatch {
        private String MatchId;
        private RoundInfo Round;
        private String MatchState;
        private Boolean HasStats;
        private Boolean HasHead2Head;
        private String MatchSequence;
        private String GroupName;
        private String DisplayTime;
        /** UTC 时间，格式 "2026-05-21 08:30:00"，可能为空字符串（Followed By 类型） */
        private String MatchTimeUtcIsoDateTime;
        private String DisplayIsoTime;
        private String NotBeforeText;
        /** 参赛类型：WC / Q / LL 等，可能为空字符串 */
        private String EntryTypeTeam1;
        /** 种子号，可能为空字符串 */
        private String SeedTeam1;
        private String EntryTypeTeam2;
        private String SeedTeam2;
        /** 主场球员（单打 Player1） */
        private PlayerInfo Player;
        /** 主场搭档（双打时非 null） */
        private PlayerInfo Partner;
        /** 对手搭档（双打时非 null） */
        private PlayerInfo OpponentPartner;
        /** 对手（单打 Player2） */
        private PlayerInfo Opponent;
        private List<Object> PossiblePlayers;
        private List<Object> PossiblePartners;
        private List<Object> PossibleOpponents;
        private List<Object> PossibleOpponentPartners;
        private MatchScores MatchScores;
        private String Winner;
        private String ResultString;
        private String MatchStateReasonMessage;
        private String Reason;
        private String Tour;
        private String PlaceholderTextOne;
        private String PlaceholderTextTwo;
        /** 获胜者 PlayerId，比赛结束后才有值 */
        private String WinningPlayerId;
    }

    @Data
    public static class RoundInfo {
        private String Id;
        private String ShortName;
        private String LongName;
    }

    @Data
    public static class PlayerInfo {
        private String PlayerId;
        private String PlayerFirstName;
        private String PlayerLastName;
        private String Country;
    }

    @Data
    public static class MatchScores {
        private String PointA;
        private String PointB;
        private String ScoreSet1A;
        private String ScoreSet1B;
        private String ScoreTBSet1;
        private String ScoreSet2A;
        private String ScoreSet2B;
        private String ScoreTBSet2;
        private String ScoreSet3A;
        private String ScoreSet3B;
        private String ScoreTBSet3;
        private String ScoreSet4A;
        private String ScoreSet4B;
        private String ScoreTBSet4;
        private String ScoreSet5A;
        private String ScoreSet5B;
        private String ScoreTBSet5;
    }
}
