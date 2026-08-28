package com.rally.domain.tournament.model;

import lombok.Getter;

import java.util.List;

/**
 * 赛事报名可用选项。
 */
@Getter
public class TournamentEntryOptionsDTO {

    private final List<String> districtOptions;
    private final List<String> timeOptions;

    public TournamentEntryOptionsDTO(List<String> districtOptions, List<String> timeOptions) {
        this.districtOptions = districtOptions == null ? List.of() : List.copyOf(districtOptions);
        this.timeOptions = timeOptions == null ? List.of() : List.copyOf(timeOptions);
    }
}
