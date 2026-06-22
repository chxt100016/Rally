package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

@Data
public class CourtMatchDTO {
    private String court;
    private String tour;
    private List<MatchQueryVO> matches;
}
