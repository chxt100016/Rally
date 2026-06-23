package com.rally.domain.tour.model;

import lombok.Data;

import java.util.List;

@Data
public class TourMatchDTO {
    private List<SeedGroupDTO> seed;
    private List<MatchGroupDTO> match;
}
