package com.rally.domain.tournament.service;

import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 匹配落地产出领域服务：分配 matchNo、创建 Match + Participant、把候选人推进为 IN_MATCH
 */
@Service
@RequiredArgsConstructor
public class TournamentMatchAssembleService {

    private final TournamentMatchRepository tournamentMatchRepository;

    private final TournamentEntryRepository tournamentEntryRepository;

    /**
     * 落地产出：为每个分组分配 matchNo、创建 Match+Participant 并持久化，把组内候选人置为 IN_MATCH
     */
    public List<TournamentMatch> assemble(String tournamentId, List<List<TournamentEntryData>> groups, TournamentRoundEnum round, int groupSize) {
        List<TournamentMatch> matches = new java.util.ArrayList<>();
        for (List<TournamentEntryData> group : groups) {
            int matchNo = tournamentMatchRepository.nextMatchNo(tournamentId);
            TournamentMatch match = TournamentMatch.createFromGroup(tournamentId, matchNo, round, groupSize, group);

            tournamentMatchRepository.save(match.getData());
            tournamentMatchRepository.saveParticipants(match.getParticipants());

            for (TournamentEntryData candidate : group) {
                candidate.setStatus(TournamentEntryStatusEnum.IN_MATCH);
                tournamentEntryRepository.save(candidate);
            }

            matches.add(match);
        }
        return matches;
    }
}
