package com.rally.client.wta.model;

import lombok.Data;

import java.util.List;

@Data
public class WtaTournamentsResponse {
    private PageInfo pageInfo;
    private List<TournamentItem> content;

    @Data
    public static class PageInfo {
        private int page;
        private int numPages;
        private int pageSize;
        private int numEntries;
    }

    @Data
    public static class TournamentItem {
        private TournamentGroup tournamentGroup;
        private int year;
        private String title;
        private String startDate;
        private String endDate;
        private String surface;
        private String inOutdoor;
        private String city;
        private String country;
        private int singlesDrawSize;
        private int doublesDrawSize;
        private long prizeMoney;
        private String prizeMoneyCurrency;
        private String liveScoringId;
        private String status;
        private String level;
    }

    @Data
    public static class TournamentGroup {
        private int id;
        private String name;
        private String level;
    }
}
