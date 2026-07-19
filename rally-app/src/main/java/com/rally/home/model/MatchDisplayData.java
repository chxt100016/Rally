package com.rally.home.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MatchDisplayData extends BaseDisplayData {
    private List<TournamentDisplayDTO> tournaments;
}
