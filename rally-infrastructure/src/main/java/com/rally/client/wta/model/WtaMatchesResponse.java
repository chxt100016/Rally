package com.rally.client.wta.model;

import lombok.Data;

import java.util.List;

@Data
public class WtaMatchesResponse {

    private List<MatchItem> matches;

    @Data
    public static class MatchItem {
        private String MatchID;
        private String EventID;
        private Integer EventYear;
        private String RoundID;
        private String DrawLevelType;
        private String DrawMatchType;
        private String PlayerIDA;
        private String PlayerIDB;
        private String PlayerNameFirstA;
        private String PlayerNameLastA;
        private String PlayerNameFirstB;
        private String PlayerNameLastB;
        private String PlayerCountryA;
        private String PlayerCountryB;
        private String SeedA;
        private String SeedB;
        private String EntryTypeA;
        private String EntryTypeB;
        private String MatchState;
        private String MatchTimeStamp;
        private String MatchTimeTotal;
        private String NotBeforeISOTime;
        private String NotBeforeText;
        private String CourtName;
        private String Winner;
        private String ScoreSet1A;
        private String ScoreSet1B;
        private String ScoreSet2A;
        private String ScoreSet2B;
        private String ScoreSet3A;
        private String ScoreSet3B;
        private String ScoreSet4A;
        private String ScoreSet4B;
        private String ScoreSet5A;
        private String ScoreSet5B;
        private String ScoreTbSet1;
        private String ScoreTbSet2;
        private String ScoreTbSet3;
        private String ScoreTbSet4;
        private String ScoreTbSet5;
    }
}
