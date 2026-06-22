package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

@Data
public class TennisMatchDTO {
    private List<SeedGroupDTO> seed;
    private List<MatchGroupDTO> match;
}
