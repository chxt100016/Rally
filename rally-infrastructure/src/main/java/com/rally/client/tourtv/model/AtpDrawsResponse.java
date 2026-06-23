package com.rally.client.tourtv.model;

import lombok.Data;

import java.util.List;

@Data
public class AtpDrawsResponse {
    private Draw MS;
    private Draw MD;
    private Draw WS;
    private Draw WD;

    @Data
    public static class Draw {
        private String EventTypeCode;
        private String Description;
        private Integer DrawSize;
        private Integer NumByes;
        private Boolean HasRoundRobin;
        private Boolean IsTeamEvent;
        private List<Round> Rounds;
    }

    @Data
    public static class Round {
        private Integer RoundId;
        private String RoundName;
        private List<Fixture> Fixtures;
    }

    @Data
    public static class Fixture {
        private String MatchCode;
        private ResultInfo Result;
        private DrawLine DrawLineTop;
        private DrawLine DrawLineBottom;
        private Boolean IsTopKnown;
        private Boolean IsBottomKnown;
        private Integer Winner;
        private String PulseStatus;
        private MatchInfo Match;
        private Metadata metadata;
    }

    @Data
    public static class Metadata {
        private String description;
    }

    @Data
    public static class ResultInfo {
        private String MatchCode;
        private Integer Winner;
        private String ResultString;
        private String ResultReason;
        private String ResultType;
        private TeamInfo TeamTop;
        private TeamInfo TeamBottom;
        private List<SetResult> SetResults;
        private String MatchTime;
    }

    @Data
    public static class SetResult {
        private Integer SetNumber;
        private Integer GamesA;
        private Integer GamesB;
        private Integer TiebreakA;
        private Integer TiebreakB;
    }

    @Data
    public static class MatchInfo {
        private String MatchId;
        private String MatchDate;
        private Integer CourtId;
        private String CourtName;
        private String MatchTime;
        private String Status;
        private String WinningPlayerId;
        private PlayerTeamInfo PlayerTeam1;
        private PlayerTeamInfo PlayerTeam2;
    }

    @Data
    public static class PlayerTeamInfo {
        private String PlayerId;
        private List<SetInfo> Sets;
    }

    @Data
    public static class SetInfo {
        private Integer SetNumber;
        private String SetScore;
    }

    @Data
    public static class TeamInfo {
        private PlayerInfo Player;
    }

    @Data
    public static class DrawLine {
        private Integer DrawLine;
        private Integer Seed;
        private List<PlayerInfo> Players;
    }

    @Data
    public static class PlayerInfo {
        private String PlayerId;
        private String FirstName;
        private String LastName;
        private String Nationality;
        private Integer OrderInTeam;
    }
}
