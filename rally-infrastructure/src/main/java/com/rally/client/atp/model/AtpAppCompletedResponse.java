package com.rally.client.atp.model;

import lombok.Data;

import java.util.List;

/**
 * app.atptour.com /api/v2/gateway/results/completed 接口返回结构
 */
@Data
public class AtpAppCompletedResponse {

    private DataWrapper Data;

    @Data
    public static class DataWrapper {
        private Integer EventYear;
        private Integer EventId;
        /** 已完成比赛列表 */
        private List<Match> Matches;
    }

    @Data
    public static class Match {
        private String MatchId;
        /** 比赛日期，格式 "2026-05-24T00:00:00" */
        private String MatchDate;
        private String CourtName;
        private Round Round;
        /** 比赛状态：F=已完成 */
        private String Status;
        /** 胜者球员 ID，用于确定胜负 */
        private String WinningPlayerId;
        private Boolean IsDoubles;
        /** 球员一（主队）信息，含比分 */
        private TeamInfo PlayerTeam;
        /** 球员二（客队）信息，含比分 */
        private TeamInfo OpponentTeam;
    }

    @Data
    public static class Round {
        private String RoundId;
        private String ShortName;
        private String LongName;
    }

    @Data
    public static class TeamInfo {
        private PlayerInfo Player;
        private Integer Seed;
        private String EntryType;
        /** 各盘比分列表，SetNumber=0 为无效数据需过滤 */
        private List<SetScoreInfo> SetScores;
    }

    @Data
    public static class PlayerInfo {
        private String PlayerId;
        private String PlayerCountry;
        private String PlayerFirstName;
        private String PlayerLastName;
    }

    @Data
    public static class SetScoreInfo {
        /** 盘号，0 为无效数据 */
        private Integer SetNumber;
        private Integer SetScore;
        private Integer TieBreakScore;
    }
}
