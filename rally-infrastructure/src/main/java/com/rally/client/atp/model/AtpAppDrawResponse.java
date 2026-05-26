package com.rally.client.atp.model;

import lombok.Data;

import java.util.List;

@Data
public class AtpAppDrawResponse {

    private Data data;

    @lombok.Data
    public static class Data {
        private Event event;
        private List<SeededPlayer> seededPlayers;
        private List<RoundResult> results;
    }

    @lombok.Data
    public static class Event {
        private Integer sglDrawSize;
    }

    @lombok.Data
    public static class SeededPlayer {
        private Integer seed;
        private String playerId;
        private String playerLastName;
        private String playerFirstName;
        private String playerNatlId;
    }

    @lombok.Data
    public static class RoundResult {
        private Round round;
        private List<Match> matches;
    }

    @lombok.Data
    public static class Round {
        private String id;
        private String shortName;
        private String longName;
    }

    @lombok.Data
    public static class Match {
        private String matchId;
        private String winningPlayerId;
        private String reason;
        private String playerId;
        private Integer playerSeed;
        private String playerEntryType;
        private String playerLastName;
        private String playerFirstName;
        private String opponentId;
        private Integer opponentSeed;
        private String opponentEntryType;
        private String opponentLastName;
        private String opponentFirstName;
        private Integer set1Player;
        private Integer set1Opponent;
        private Integer set1Tie;
        private Integer set2Player;
        private Integer set2Opponent;
        private Integer set2Tie;
        private Integer set3Player;
        private Integer set3Opponent;
        private Integer set3Tie;
        private Integer set4Player;
        private Integer set4Opponent;
        private Integer set4Tie;
        private Integer set5Player;
        private Integer set5Opponent;
        private Integer set5Tie;
    }
}
