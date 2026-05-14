package com.rally.client.wta.model;

import lombok.Data;

import java.util.List;

@Data
public class WtaRankingsResponse {

    private RankingData Data;

    @Data
    public static class RankingData {
        private RankingList Rankings;
    }

    @Data
    public static class RankingList {
        private List<PlayerRanking> Players;
    }

    @Data
    public static class PlayerRanking {
        private String PlayerId;
        private String LastName;
        private String FirstName;
        private String NatlId;
        private Integer Rank;
        private Integer Points;
        private String BirthDate;
    }
}
