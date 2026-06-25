package com.rally.domain.recap.model;

import lombok.Data;

@Data
public class ScoreListQueryCmd {

    private Result result;
    private MatchType matchType;
    private String lastId;
    private Integer pageSize;

    public enum Result { WIN, LOSE }
    public enum MatchType { SINGLE, DOUBLE }
}
