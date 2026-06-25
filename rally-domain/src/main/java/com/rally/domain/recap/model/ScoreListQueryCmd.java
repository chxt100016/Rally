package com.rally.domain.recap.model;

import lombok.Data;

@Data
public class ScoreListQueryCmd {

    private MatchType matchType;
    private String meetupId;
    private String lastId;
    private Integer pageSize;

    public enum MatchType { SINGLE, DOUBLE }
}
