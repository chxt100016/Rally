package com.rally.client.atp.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class AtpTournamentsResponse {

    @JSONField(name = "TournamentDates")
    private List<TournamentDate> tournamentDates;

    @Data
    public static class TournamentDate {
        @JSONField(name = "Tournaments")
        private List<TournamentItem> tournaments;
    }

    @Data
    public static class TournamentItem {
        @JSONField(name = "Id")
        private String id;

        @JSONField(name = "Name")
        private String name;

        /** 城市, 国家 */
        @JSONField(name = "Location")
        private String location;

        /** 如 "4 - 11 January, 2026" */
        @JSONField(name = "FormattedDate")
        private String formattedDate;

        @JSONField(name = "IsPastEvent")
        private Boolean isPastEvent;

        /** 赛事级别，如 "250", "500", "1000", "GS", "UC" */
        @JSONField(name = "Type")
        private String type;

        @JSONField(name = "Surface")
        private String surface;

        /** 总奖金，如 "$888,349" */
        @JSONField(name = "TotalFinancialCommitment")
        private String totalFinancialCommitment;
    }
}
