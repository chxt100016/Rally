package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MatchQueryResponse {

    private List<SeedVO> seeds;
    private Map<String, List<MatchQueryVO>> matches;
}
