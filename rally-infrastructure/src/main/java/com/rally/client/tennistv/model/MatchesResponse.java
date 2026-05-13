package com.rally.client.tennistv.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class MatchesResponse {
    private List<TournamentInfo> tournaments;
    private List<MatchInfo> matches;

    @Data
    public static class TournamentInfo {
        private String id;
        private Integer year;
        private String name;
        private String gender;
        private String start;
        private String end;
        private String type;
        private String location;
        private String surface;
        private TournamentDetailInfo info;
        private List<OopDay> availableOopDays;
        private TournamentMetadata metadata;
    }

    @Data
    public static class TournamentDetailInfo {
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
        private Integer dateSeq;
        private String dateLabel;
        private Boolean qualifying;
        private String isoDate;
        private String isoUtcDate;
    }

    @Data
    public static class TournamentMetadata {
        private String customRoute;
        private String matchesSize;
    }

    @Data
    public static class MatchInfo {
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
        @JSONField(name = "Status")
        private String status;
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
        @JSONField(name = "onDemandUrl")
        private String onDemandUrl;
        @JSONField(name = "metadata")
        private MatchMetadata metadata;
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
        @JSONField(name = "GamePointsPlayerTeam")
        private String gamePointsPlayerTeam;
        @JSONField(name = "Sets")
        private List<SetInfo> sets;
    }

    @Data
    public static class SetInfo {
        @JSONField(name = "SetNumber")
        private Integer setNumber;
        @JSONField(name = "SetScore")
        private String setScore;
        @JSONField(name = "TieBreakScore")
        private String tieBreakScore;
    }

    @Data
    public static class MatchMetadata {
        private String description;
    }
}
