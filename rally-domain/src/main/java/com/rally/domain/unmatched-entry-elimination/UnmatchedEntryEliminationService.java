package com.rally.domain.tournament.unmatchedentryelimination;

import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure evaluator for deciding whether one tournament entry may be eliminated.
 *
 * <p>The result is only a preflight decision. This service does not read or write
 * persistence, mutate the supplied snapshot, or inspect linked entries.</p>
 */
@Service
public class UnmatchedEntryEliminationService {

    private static final Set<TournamentEntryStatusEnum> ELIGIBLE_STATUSES = Set.of(
            TournamentEntryStatusEnum.WAITING,
            TournamentEntryStatusEnum.FROZEN);

    public SingleEntryEliminationDecision evaluate(
            TournamentRoundEnum tournamentCurrentRound,
            SingleEntrySnapshot entry,
            Boolean inActiveMatch) {
        // R1-R2: reject incomplete context before inspecting business state.
        if (tournamentCurrentRound == null
                || entry == null
                || inActiveMatch == null
                || entry.userId() == null
                || entry.userId().isBlank()) {
            return SingleEntryEliminationDecision.INPUT_INVALID;
        }

        // R3: the target must still be an eligible entry in the tournament round.
        if (entry.currentRound() != tournamentCurrentRound
                || !ELIGIBLE_STATUSES.contains(entry.status())) {
            return SingleEntryEliminationDecision.ENTRY_STATUS_OR_ROUND_INVALID;
        }

        // R4: an active participation fact takes precedence over eligibility.
        if (inActiveMatch) {
            return SingleEntryEliminationDecision.IN_ACTIVE_MATCH;
        }
        // R5-R6: pure, side-effect-free accepted decision.
        return SingleEntryEliminationDecision.ELIGIBLE;
    }

    /**
     * @deprecated Compatibility bridge for the retired batch-entry contract.
     * New callers must use the single-entry overload.
     */
    @Deprecated
    public UnmatchedEntryEliminationResult evaluate(
            com.rally.domain.meetup.enums.MatchTypeEnum matchType,
            TournamentRoundEnum currentRound,
            List<UnmatchedEntrySnapshot> entries,
            List<ActiveParticipantSnapshot> activeParticipants) {
        if (!isSupported(matchType)
                || currentRound == null
                || entries == null
                || activeParticipants == null) {
            return UnmatchedEntryEliminationResult.rejectedInputInvalid();
        }
        if (entries.isEmpty()) {
            return UnmatchedEntryEliminationResult.accepted(List.of(), List.of());
        }

        Map<Integer, List<UnmatchedEntrySnapshot>> entriesByNo = groupEntries(entries);
        Map<String, Set<Integer>> entryNosByUser = indexEntryNosByUser(entriesByNo);
        Set<String> activeUserIds = new HashSet<>();
        Set<Integer> activeEntryNos = new HashSet<>();
        indexActiveParticipants(activeParticipants, activeUserIds, activeEntryNos);

        List<Integer> candidates = new ArrayList<>();
        List<Integer> excluded = new ArrayList<>();
        for (Map.Entry<Integer, List<UnmatchedEntrySnapshot>> group : entriesByNo.entrySet()) {
            Integer entryNo = group.getKey();
            List<UnmatchedEntrySnapshot> members = group.getValue();
            if (hasValidStructure(matchType, members)
                    && hasEligibleState(currentRound, members)
                    && !hasDuplicateUserAcrossUnits(members, entryNosByUser)
                    && !isActive(entryNo, members, activeEntryNos, activeUserIds)) {
                candidates.add(entryNo);
            } else {
                excluded.add(entryNo);
            }
        }
        return UnmatchedEntryEliminationResult.accepted(candidates, excluded);
    }

    private boolean isSupported(com.rally.domain.meetup.enums.MatchTypeEnum matchType) {
        return matchType == com.rally.domain.meetup.enums.MatchTypeEnum.SINGLE
                || matchType == com.rally.domain.meetup.enums.MatchTypeEnum.DOUBLE;
    }

    private Map<Integer, List<UnmatchedEntrySnapshot>> groupEntries(
            List<UnmatchedEntrySnapshot> entries) {
        Map<Integer, List<UnmatchedEntrySnapshot>> grouped = new TreeMap<>();
        for (UnmatchedEntrySnapshot entry : entries) {
            if (entry == null || !isPositive(entry.entryNo())) {
                continue;
            }
            grouped.computeIfAbsent(entry.entryNo(), ignored -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    private Map<String, Set<Integer>> indexEntryNosByUser(
            Map<Integer, List<UnmatchedEntrySnapshot>> entriesByNo) {
        Map<String, Set<Integer>> entryNosByUser = new HashMap<>();
        for (Map.Entry<Integer, List<UnmatchedEntrySnapshot>> group : entriesByNo.entrySet()) {
            for (UnmatchedEntrySnapshot entry : group.getValue()) {
                if (hasText(entry.userId())) {
                    entryNosByUser.computeIfAbsent(entry.userId(), ignored -> new HashSet<>())
                            .add(group.getKey());
                }
            }
        }
        return entryNosByUser;
    }

    private void indexActiveParticipants(
            List<ActiveParticipantSnapshot> participants,
            Set<String> activeUserIds,
            Set<Integer> activeEntryNos) {
        for (ActiveParticipantSnapshot participant : participants) {
            if (participant == null) {
                continue;
            }
            if (hasText(participant.userId())) {
                activeUserIds.add(participant.userId());
            }
            if (isPositive(participant.entryNo())) {
                activeEntryNos.add(participant.entryNo());
            }
        }
    }

    private boolean hasValidStructure(
            com.rally.domain.meetup.enums.MatchTypeEnum matchType,
            List<UnmatchedEntrySnapshot> members) {
        if (matchType == com.rally.domain.meetup.enums.MatchTypeEnum.SINGLE) {
            return members.size() == 1 && hasText(members.get(0).userId());
        }
        if (members.size() != 2) {
            return false;
        }
        UnmatchedEntrySnapshot first = members.get(0);
        UnmatchedEntrySnapshot second = members.get(1);
        return hasText(first.userId())
                && hasText(second.userId())
                && !Objects.equals(first.userId(), second.userId())
                && hasText(first.partnerId())
                && hasText(second.partnerId())
                && Objects.equals(first.partnerId(), second.userId())
                && Objects.equals(second.partnerId(), first.userId());
    }

    private boolean hasEligibleState(
            TournamentRoundEnum currentRound,
            List<UnmatchedEntrySnapshot> members) {
        for (UnmatchedEntrySnapshot member : members) {
            if (member.currentRound() != currentRound
                    || !ELIGIBLE_STATUSES.contains(member.status())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasDuplicateUserAcrossUnits(
            List<UnmatchedEntrySnapshot> members,
            Map<String, Set<Integer>> entryNosByUser) {
        for (UnmatchedEntrySnapshot member : members) {
            Set<Integer> entryNos = entryNosByUser.get(member.userId());
            if (entryNos != null && entryNos.size() > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isActive(
            Integer entryNo,
            List<UnmatchedEntrySnapshot> members,
            Set<Integer> activeEntryNos,
            Set<String> activeUserIds) {
        if (activeEntryNos.contains(entryNo)) {
            return true;
        }
        for (UnmatchedEntrySnapshot member : members) {
            if (activeUserIds.contains(member.userId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
