package com.rally.client.atp.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class AtpAppDrawResponse {

    @JSONField(name = "Data")
    private Data data;

    @lombok.Data
    public static class Data {
        @JSONField(name = "Event")
        private Event event;
        @JSONField(name = "SeededPlayers")
        private List<SeededPlayer> seededPlayers;
        @JSONField(name = "Results")
        private List<RoundResult> results;
        @JSONField(name = "Draw")
        private List<DrawEntry> draw;
    }

    @lombok.Data
    public static class Event {
        @JSONField(name = "SglDrawSize")
        private Integer sglDrawSize;
    }

    @lombok.Data
    public static class DrawEntry {
        @JSONField(name = "DrawPosition")
        private Integer drawPosition;
        @JSONField(name = "PlayerId")
        private String playerId;
        @JSONField(name = "PlayerLastName")
        private String playerLastName;
        @JSONField(name = "PlayerFirstName")
        private String playerFirstName;
        @JSONField(name = "PlayerNatlId")
        private String playerNatlId;
        @JSONField(name = "Seed")
        private Integer seed;
        @JSONField(name = "EntryType")
        private String entryType;
    }

    @lombok.Data
    public static class SeededPlayer {
        @JSONField(name = "Seed")
        private Integer seed;
        @JSONField(name = "PlayerId")
        private String playerId;
        @JSONField(name = "PlayerLastName")
        private String playerLastName;
        @JSONField(name = "PlayerFirstName")
        private String playerFirstName;
        @JSONField(name = "PlayerNatlId")
        private String playerNatlId;
    }

    @lombok.Data
    public static class RoundResult {
        @JSONField(name = "Round")
        private Round round;
        @JSONField(name = "Matches")
        private List<Match> matches;
    }

    @lombok.Data
    public static class Round {
        @JSONField(name = "Id")
        private String id;
        @JSONField(name = "ShortName")
        private String shortName;
        @JSONField(name = "LongName")
        private String longName;
    }

    @lombok.Data
    public static class Match {
        @JSONField(name = "MatchId")
        private String matchId;
        @JSONField(name = "WinningPlayerId")
        private String winningPlayerId;
        @JSONField(name = "Reason")
        private String reason;
        @JSONField(name = "PlayerId")
        private String playerId;
        @JSONField(name = "PlayerSeed")
        private Integer playerSeed;
        @JSONField(name = "PlayerEntryType")
        private String playerEntryType;
        @JSONField(name = "PlayerLastName")
        private String playerLastName;
        @JSONField(name = "PlayerFirstName")
        private String playerFirstName;
        @JSONField(name = "OpponentId")
        private String opponentId;
        @JSONField(name = "OpponentSeed")
        private Integer opponentSeed;
        @JSONField(name = "OpponentEntryType")
        private String opponentEntryType;
        @JSONField(name = "OpponentLastName")
        private String opponentLastName;
        @JSONField(name = "OpponentFirstName")
        private String opponentFirstName;
        @JSONField(name = "Set1Player")
        private Integer set1Player;
        @JSONField(name = "Set1Opponent")
        private Integer set1Opponent;
        @JSONField(name = "Set1Tie")
        private Integer set1Tie;
        @JSONField(name = "Set2Player")
        private Integer set2Player;
        @JSONField(name = "Set2Opponent")
        private Integer set2Opponent;
        @JSONField(name = "Set2Tie")
        private Integer set2Tie;
        @JSONField(name = "Set3Player")
        private Integer set3Player;
        @JSONField(name = "Set3Opponent")
        private Integer set3Opponent;
        @JSONField(name = "Set3Tie")
        private Integer set3Tie;
        @JSONField(name = "Set4Player")
        private Integer set4Player;
        @JSONField(name = "Set4Opponent")
        private Integer set4Opponent;
        @JSONField(name = "Set4Tie")
        private Integer set4Tie;
        @JSONField(name = "Set5Player")
        private Integer set5Player;
        @JSONField(name = "Set5Opponent")
        private Integer set5Opponent;
        @JSONField(name = "Set5Tie")
        private Integer set5Tie;
    }
}
