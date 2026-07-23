package com.rally.domain.tournament.service;

import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 匹配落地产出领域服务：分配 matchNo、创建 Match + Participant、把候选人推进为 IN_MATCH
 */
@Service
@RequiredArgsConstructor
public class TournamentMatchAssembleService {

    private final TournamentMatchRepository tournamentMatchRepository;

    private final TournamentEntryRepository tournamentEntryRepository;

    /**
     * 构建拒绝历史查询：查该赛事下所有 REJECTED 比赛的参与者，按 matchId 分组后两两互相拒绝配对
     */
    public RejectHistoryLookup buildRejectHistoryLookup(String tournamentId) {
        List<MatchParticipantData> rejectedParticipants = tournamentMatchRepository.findRejectedParticipantsByTournament(tournamentId);
        Map<String, List<String>> matchIdToUserIds = new HashMap<>();
        for (MatchParticipantData participant : rejectedParticipants) {
            matchIdToUserIds.computeIfAbsent(participant.getMatchId(), k -> new java.util.ArrayList<>()).add(participant.getUserId());
        }
        Set<String> rejectedPairs = new HashSet<>();
        for (List<String> userIds : matchIdToUserIds.values()) {
            for (int i = 0; i < userIds.size(); i++) {
                for (int j = i + 1; j < userIds.size(); j++) {
                    rejectedPairs.add(pairKey(userIds.get(i), userIds.get(j)));
                }
            }
        }
        return (userIdA, userIdB) -> rejectedPairs.contains(pairKey(userIdA, userIdB));
    }

    private String pairKey(String userIdA, String userIdB) {
        return userIdA.compareTo(userIdB) <= 0 ? userIdA + "|" + userIdB : userIdB + "|" + userIdA;
    }

    /**
     * 落地产出：为每个分组分配 matchNo、创建 Match+Participant 并持久化，把组内候选人置为 IN_MATCH
     */
    public List<TournamentMatch> assemble(String tournamentId, List<MatchGroup> groups, TournamentRoundEnum round, int groupSize) {
        List<TournamentMatch> matches = new java.util.ArrayList<>();
        for (MatchGroup group : groups) {
            int matchNo = tournamentMatchRepository.nextMatchNo(tournamentId);
            TournamentMatch match = TournamentMatch.createFromGroup(tournamentId, matchNo, round, groupSize, group.getMembers());

            tournamentMatchRepository.save(match.getData());
            tournamentMatchRepository.saveParticipants(match.getParticipants());

            for (TournamentEntryData candidate : group.getMembers()) {
                candidate.setStatus(TournamentEntryStatusEnum.IN_MATCH);
                tournamentEntryRepository.save(candidate);
            }

            matches.add(match);
        }
        return matches;
    }
}
