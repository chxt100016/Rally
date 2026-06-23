package com.rally.client.tourtv.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AtpOopResponse {

    private Integer id;
    private Integer year;
    private String name;
    private String gender;
    private String start;
    private String end;
    private String type;
    private String location;
    private String surface;
    private InfoDetail info;
    private List<OopDay> oop;

    @Data
    public static class InfoDetail {
        private String title;
        private String city;
        private String inOutdoor;
        private Double utcOffset;
        private String qualStart;
        private String mainStart;
        private Integer drawSizeSM;
        private Integer drawSizeDM;
        private Integer drawSizeSQ;
        private Integer drawSizeDQ;
        private String commitment;
        private String prize;
        private String tdiId;
    }

    @Data
    public static class OopDay {
        @JSONField(name = "DateSeq")
        private Integer dateSeq;
        @JSONField(name = "Courts")
        private Map<String, CourtDetail> courts;
    }

    @Data
    public static class CourtDetail {
        @JSONField(name = "CourtId")
        private Integer courtId;
        @JSONField(name = "CourtName")
        private String courtName;
        @JSONField(name = "Matches")
        private List<MatchDetail> matches;
    }

    @Data
    public static class MatchDetail {
        @JSONField(name = "MatchId")
        private String matchId;
        @JSONField(name = "AssociationCode")
        private String associationCode;
        @JSONField(name = "UmpireFirstName")
        private String umpireFirstName;
        @JSONField(name = "UmpireLastName")
        private String umpireLastName;
        @JSONField(name = "DateSeq")
        private Integer dateSeq;
        @JSONField(name = "MatchDate")
        private String matchDate;
        @JSONField(name = "CourtId")
        private Integer courtId;
        @JSONField(name = "CourtName")
        private String courtName;
        @JSONField(name = "Round")
        private RoundInfo round;
        @JSONField(name = "MatchTime")
        private String matchTime;
        @JSONField(name = "NumberOfSets")
        private Integer numberOfSets;
        @JSONField(name = "Status")
        private String status;
        @JSONField(name = "WinningPlayerId")
        private String winningPlayerId;
        @JSONField(name = "Serve")
        private String serve;
        @JSONField(name = "PlayerTeam1")
        private PlayerTeam playerTeam1;
        @JSONField(name = "PlayerTeam2")
        private PlayerTeam playerTeam2;
        @JSONField(name = "TournamentId")
        private Integer tournamentId;
        @JSONField(name = "TournamentYear")
        private Integer tournamentYear;
        @JSONField(name = "PulseStatus")
        private String pulseStatus;
        @JSONField(name = "CourtSeq")
        private Integer courtSeq;
        private String onDemandUrl;
        @JSONField(name = "NotBeforeISOTime")
        private String notBeforeISOTime;
        @JSONField(name = "NotBeforeText")
        private String notBeforeText;
    }

    @Data
    public static class RoundInfo {
        @JSONField(name = "LongName")
        private String longName;
    }

    @Data
    public static class PlayerTeam {
        @JSONField(name = "PlayerId")
        private String playerId;
        @JSONField(name = "PlayerFirstName")
        private String playerFirstName;
        @JSONField(name = "PlayerFirstNameFull")
        private String playerFirstNameFull;
        @JSONField(name = "PlayerLastName")
        private String playerLastName;
        @JSONField(name = "PlayerCountryCode")
        private String playerCountryCode;
        @JSONField(name = "SeedPlayerTeam")
        private String seedPlayerTeam;
        @JSONField(name = "Sets")
        private List<SetScore> sets;
    }

    @Data
    public static class SetScore {
        @JSONField(name = "SetNumber")
        private Integer setNumber;
        @JSONField(name = "SetScore")
        private String setScore;
    }
}
