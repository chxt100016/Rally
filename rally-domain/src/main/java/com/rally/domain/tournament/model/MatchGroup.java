package com.rally.domain.tournament.model;

import lombok.Data;

import java.util.List;

/**
 * 匹配算法产出的一组候选人（纯内存对象，非持久化）
 */
@Data
public class MatchGroup {
    private List<TournamentEntryData> members;

    public MatchGroup(List<TournamentEntryData> members) {
        this.members = members;
    }
}
