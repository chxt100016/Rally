package com.rally.domain.tournament.matchmaking;

import com.rally.domain.tournament.entry.TournamentEntryStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure current-round tournament matchmaking. The service validates manual
 * intent first and then performs an exact, deterministic maximum-coverage
 * search over the remaining entry units.
 */
@Service
public class TournamentMatchmakingService {

    public MatchmakingResult match(MatchmakingRequest request) {
        MatchmakingRejection rejection = validateRequest(request);
        if (rejection != null) {
            return MatchmakingResult.rejected(rejection);
        }

        Set<Integer> excluded = request.excludedEntryNos() == null
                ? Set.of() : request.excludedEntryNos();
        Map<Integer, MatchmakingCandidate> availableByEntryNo = new LinkedHashMap<>();
        request.candidates().stream()
                .filter(candidate -> !excluded.contains(candidate.entryNo()))
                .sorted(Comparator.comparing(MatchmakingCandidate::entryNo))
                .forEach(candidate -> availableByEntryNo.put(candidate.entryNo(), candidate));

        ManualSelection manual = selectManualGroups(
                request.manualGroups(), request.groupSize(), availableByEntryNo);
        if (manual.rejection != null) {
            return MatchmakingResult.rejected(manual.rejection);
        }

        List<MatchmakingCandidate> automaticCandidates = availableByEntryNo.values().stream()
                .filter(candidate -> !manual.usedEntryNos.contains(candidate.entryNo()))
                .toList();
        Set<EntryPair> completedPairs = normalizedCompletedPairs(request.completedPairings());
        List<FeasibleGroup> feasibleGroups = enumerateFeasibleGroups(
                automaticCandidates, request.groupSize(), completedPairs);
        Plan bestPlan = selectBestPlan(automaticCandidates, feasibleGroups);

        List<MatchmakingGroup> groups = new ArrayList<>(manual.groups);
        bestPlan.groups.stream()
                .sorted(Comparator.comparing(FeasibleGroup::entrySignature))
                .map(FeasibleGroup::toResult)
                .forEach(groups::add);

        Set<Integer> matchedEntryNos = new HashSet<>(manual.usedEntryNos);
        bestPlan.groups.forEach(group -> group.indices.forEach(
                index -> matchedEntryNos.add(automaticCandidates.get(index).entryNo())));
        List<Integer> unmatched = availableByEntryNo.keySet().stream()
                .filter(entryNo -> !matchedEntryNos.contains(entryNo))
                .sorted()
                .toList();
        return MatchmakingResult.accepted(groups, unmatched);
    }

    private MatchmakingRejection validateRequest(MatchmakingRequest request) {
        if (request == null) {
            return MatchmakingRejection.REQUEST_REQUIRED;
        }
        if (request.round() == null) {
            return MatchmakingRejection.ROUND_REQUIRED;
        }
        if (request.groupSize() != 2 && request.groupSize() != 3) {
            return MatchmakingRejection.GROUP_SIZE_UNSUPPORTED;
        }
        if (request.candidates() == null) {
            return MatchmakingRejection.CANDIDATES_REQUIRED;
        }

        Set<Integer> entryNos = new HashSet<>();
        for (MatchmakingCandidate candidate : request.candidates()) {
            if (candidate == null || candidate.entryNo() == null || candidate.entryNo() <= 0
                    || candidate.members() == null || candidate.commonAvailableTimes() == null
                    || candidate.commonDistricts() == null) {
                return MatchmakingRejection.CANDIDATE_INVALID;
            }
            if (!entryNos.add(candidate.entryNo())) {
                return MatchmakingRejection.CANDIDATE_DUPLICATED;
            }
            if (!isComplete(candidate)) {
                return MatchmakingRejection.CANDIDATE_INCOMPLETE;
            }
            if (candidate.status() != TournamentEntryStatus.WAITING) {
                return MatchmakingRejection.CANDIDATE_NOT_WAITING;
            }
            if (candidate.round() != request.round()) {
                return MatchmakingRejection.CANDIDATE_ROUND_MISMATCH;
            }
            if (containsBlank(candidate.commonAvailableTimes())
                    || containsBlank(candidate.commonDistricts())) {
                return MatchmakingRejection.CANDIDATE_INVALID;
            }
        }

        if (request.excludedEntryNos() != null && request.excludedEntryNos().contains(null)) {
            return MatchmakingRejection.CANDIDATE_INVALID;
        }
        if (request.completedPairings() != null) {
            for (CompletedPairing pairing : request.completedPairings()) {
                if (pairing == null || pairing.leftEntryNo() == null || pairing.rightEntryNo() == null
                        || pairing.leftEntryNo().equals(pairing.rightEntryNo())) {
                    return MatchmakingRejection.COMPLETED_PAIRING_INVALID;
                }
            }
        }
        return null;
    }

    private boolean isComplete(MatchmakingCandidate candidate) {
        if (candidate.requiredMemberCount() <= 0
                || candidate.members().size() != candidate.requiredMemberCount()) {
            return false;
        }
        Set<String> userIds = new HashSet<>();
        for (MatchmakingMember member : candidate.members()) {
            if (member == null || isBlank(member.userId()) || !userIds.add(member.userId())) {
                return false;
            }
        }
        return true;
    }

    private boolean containsBlank(Collection<String> values) {
        return values.stream().anyMatch(this::isBlank);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ManualSelection selectManualGroups(
            List<List<Integer>> requestedGroups,
            int groupSize,
            Map<Integer, MatchmakingCandidate> candidates) {
        if (requestedGroups == null || requestedGroups.isEmpty()) {
            return ManualSelection.empty();
        }
        List<MatchmakingGroup> groups = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (List<Integer> requestedGroup : requestedGroups) {
            if (requestedGroup == null || requestedGroup.size() != groupSize) {
                return ManualSelection.rejected();
            }
            Set<Integer> withinGroup = new HashSet<>();
            List<MatchmakingCandidate> members = new ArrayList<>(groupSize);
            for (Integer entryNo : requestedGroup) {
                if (entryNo == null || !withinGroup.add(entryNo) || !used.add(entryNo)) {
                    return ManualSelection.rejected();
                }
                MatchmakingCandidate candidate = candidates.get(entryNo);
                if (candidate == null) {
                    return ManualSelection.rejected();
                }
                members.add(candidate);
            }
            groups.add(toResult(requestedGroup, members));
        }
        return new ManualSelection(groups, used, null);
    }

    private Set<EntryPair> normalizedCompletedPairs(Set<CompletedPairing> pairings) {
        if (pairings == null || pairings.isEmpty()) {
            return Set.of();
        }
        Set<EntryPair> normalized = new HashSet<>();
        for (CompletedPairing pairing : pairings) {
            normalized.add(EntryPair.of(pairing.leftEntryNo(), pairing.rightEntryNo()));
        }
        return normalized;
    }

    private List<FeasibleGroup> enumerateFeasibleGroups(
            List<MatchmakingCandidate> candidates,
            int groupSize,
            Set<EntryPair> completedPairs) {
        List<FeasibleGroup> groups = new ArrayList<>();
        enumerate(candidates, groupSize, completedPairs, 0, new ArrayList<>(), groups);
        groups.sort(Comparator.comparing(FeasibleGroup::entrySignature));
        return groups;
    }

    private void enumerate(
            List<MatchmakingCandidate> candidates,
            int groupSize,
            Set<EntryPair> completedPairs,
            int start,
            List<Integer> selected,
            List<FeasibleGroup> result) {
        if (selected.size() == groupSize) {
            List<MatchmakingCandidate> members = selected.stream().map(candidates::get).toList();
            if (hasCommonValue(members, MatchmakingCandidate::commonAvailableTimes)
                    && hasCommonValue(members, MatchmakingCandidate::commonDistricts)
                    && hasNoCompletedPairing(members, completedPairs)) {
                result.add(new FeasibleGroup(selected, members));
            }
            return;
        }
        for (int index = start; index < candidates.size(); index++) {
            selected.add(index);
            enumerate(candidates, groupSize, completedPairs, index + 1, selected, result);
            selected.remove(selected.size() - 1);
        }
    }

    private boolean hasNoCompletedPairing(
            List<MatchmakingCandidate> candidates,
            Set<EntryPair> completedPairs) {
        for (int left = 0; left < candidates.size(); left++) {
            for (int right = left + 1; right < candidates.size(); right++) {
                if (completedPairs.contains(EntryPair.of(
                        candidates.get(left).entryNo(), candidates.get(right).entryNo()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private Plan selectBestPlan(
            List<MatchmakingCandidate> candidates,
            List<FeasibleGroup> groups) {
        Map<Integer, List<FeasibleGroup>> byCandidate = new HashMap<>();
        for (FeasibleGroup group : groups) {
            for (Integer index : group.indices) {
                byCandidate.computeIfAbsent(index, ignored -> new ArrayList<>()).add(group);
            }
        }
        Plan best = new Plan(candidates);
        search(candidates, byCandidate, 0, new Plan(candidates), best);
        return best;
    }

    private void search(
            List<MatchmakingCandidate> candidates,
            Map<Integer, List<FeasibleGroup>> byCandidate,
            int index,
            Plan current,
            Plan best) {
        while (index < candidates.size() && current.used.get(index)) {
            index++;
        }
        if (index >= candidates.size()) {
            if (current.betterThan(best)) {
                best.copyFrom(current);
            }
            return;
        }
        int remaining = candidates.size() - index;
        if (current.used.cardinality() + remaining < best.used.cardinality()) {
            return;
        }

        // The current entry may remain unmatched in the globally best plan.
        search(candidates, byCandidate, index + 1, current, best);
        for (FeasibleGroup group : byCandidate.getOrDefault(index, List.of())) {
            // A skipped earlier entry is final; only branch on a group's first
            // (and therefore lowest) index so every plan is explored once.
            if (group.indices.get(0) == index && current.disjoint(group)) {
                current.add(group);
                search(candidates, byCandidate, index + 1, current, best);
                current.remove(group);
            }
        }
    }

    private static boolean hasCommonValue(
            List<MatchmakingCandidate> candidates,
            java.util.function.Function<MatchmakingCandidate, Set<String>> extractor) {
        Set<String> common = null;
        for (MatchmakingCandidate candidate : candidates) {
            Set<String> values = extractor.apply(candidate);
            if (values == null || values.isEmpty()) {
                return false;
            }
            if (common == null) {
                common = new HashSet<>(values);
            } else {
                common.retainAll(values);
            }
            if (common.isEmpty()) {
                return false;
            }
        }
        return common != null && !common.isEmpty();
    }

    private static MatchmakingGroup toResult(
            List<Integer> entryNos,
            List<MatchmakingCandidate> candidates) {
        List<String> commonTimes = commonValues(
                candidates, MatchmakingCandidate::commonAvailableTimes);
        List<String> commonDistricts = commonValues(
                candidates, MatchmakingCandidate::commonDistricts);
        Set<String> courtBookers = new LinkedHashSet<>();
        candidates.stream().flatMap(candidate -> candidate.members().stream())
                .filter(MatchmakingMember::canBookCourt)
                .map(MatchmakingMember::userId)
                .forEach(courtBookers::add);
        String courtBookerId = courtBookers.size() == 1
                ? courtBookers.iterator().next() : null;
        return new MatchmakingGroup(entryNos, commonTimes, commonDistricts, courtBookerId);
    }

    private static List<String> commonValues(
            List<MatchmakingCandidate> candidates,
            java.util.function.Function<MatchmakingCandidate, Set<String>> extractor) {
        Set<String> common = null;
        for (MatchmakingCandidate candidate : candidates) {
            Set<String> values = extractor.apply(candidate);
            if (common == null) {
                common = new HashSet<>(values);
            } else {
                common.retainAll(values);
            }
        }
        return common == null ? List.of() : common.stream().sorted().toList();
    }

    private record EntryPair(int lower, int upper) {
        static EntryPair of(int left, int right) {
            return left < right ? new EntryPair(left, right) : new EntryPair(right, left);
        }
    }

    private static final class ManualSelection {
        private final List<MatchmakingGroup> groups;
        private final Set<Integer> usedEntryNos;
        private final MatchmakingRejection rejection;

        private ManualSelection(
                List<MatchmakingGroup> groups,
                Set<Integer> usedEntryNos,
                MatchmakingRejection rejection) {
            this.groups = groups;
            this.usedEntryNos = usedEntryNos;
            this.rejection = rejection;
        }

        private static ManualSelection empty() {
            return new ManualSelection(List.of(), Set.of(), null);
        }

        private static ManualSelection rejected() {
            return new ManualSelection(
                    List.of(), Set.of(), MatchmakingRejection.MANUAL_GROUP_INVALID);
        }
    }

    private static final class FeasibleGroup {
        private final List<Integer> indices;
        private final List<MatchmakingCandidate> candidates;
        private final String entrySignature;
        private final boolean hasUniqueCourtBooker;
        private final boolean hasExpectedGenderComposition;

        private FeasibleGroup(
                List<Integer> selected,
                List<MatchmakingCandidate> candidates) {
            this.indices = List.copyOf(selected);
            this.candidates = List.copyOf(candidates);
            this.entrySignature = candidates.stream()
                    .map(candidate -> candidate.entryNo().toString())
                    .sorted()
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("");
            this.hasUniqueCourtBooker = uniqueCourtBookerCount(candidates) == 1;
            this.hasExpectedGenderComposition = hasSameGenderComposition(candidates);
        }

        private static int uniqueCourtBookerCount(List<MatchmakingCandidate> candidates) {
            return (int) candidates.stream()
                    .flatMap(candidate -> candidate.members().stream())
                    .filter(MatchmakingMember::canBookCourt)
                    .map(MatchmakingMember::userId)
                    .distinct()
                    .count();
        }

        private static boolean hasSameGenderComposition(List<MatchmakingCandidate> candidates) {
            return candidates.stream().map(candidate -> candidate.members().stream()
                            .map(member -> member.gender() == null ? "UNKNOWN" : member.gender())
                            .sorted()
                            .reduce((left, right) -> left + "|" + right)
                            .orElse("UNKNOWN"))
                    .distinct().count() == 1;
        }

        private String entrySignature() {
            return entrySignature;
        }

        private MatchmakingGroup toResult() {
            return TournamentMatchmakingService.toResult(
                    candidates.stream().map(MatchmakingCandidate::entryNo).sorted().toList(),
                    candidates);
        }
    }

    private static final class Plan {
        private final List<MatchmakingCandidate> candidates;
        private final List<FeasibleGroup> groups = new ArrayList<>();
        private final BitSet used = new BitSet();

        private Plan(List<MatchmakingCandidate> candidates) {
            this.candidates = candidates;
        }

        private boolean disjoint(FeasibleGroup group) {
            return group.indices.stream().noneMatch(used::get);
        }

        private void add(FeasibleGroup group) {
            groups.add(group);
            group.indices.forEach(used::set);
        }

        private void remove(FeasibleGroup group) {
            groups.remove(groups.size() - 1);
            group.indices.forEach(used::clear);
        }

        private boolean betterThan(Plan other) {
            int coverage = Integer.compare(used.cardinality(), other.used.cardinality());
            if (coverage != 0) {
                return coverage > 0;
            }
            int uniqueBookerGroups = Long.compare(
                    groups.stream().filter(group -> group.hasUniqueCourtBooker).count(),
                    other.groups.stream().filter(group -> group.hasUniqueCourtBooker).count());
            if (uniqueBookerGroups != 0) {
                return uniqueBookerGroups > 0;
            }
            int expectedGenderGroups = Long.compare(
                    groups.stream().filter(group -> group.hasExpectedGenderComposition).count(),
                    other.groups.stream().filter(group -> group.hasExpectedGenderComposition).count());
            if (expectedGenderGroups != 0) {
                return expectedGenderGroups > 0;
            }
            int joinedComparison = compareJoinedTimes(joinedTimes(), other.joinedTimes());
            if (joinedComparison != 0) {
                return joinedComparison < 0;
            }
            return planSignature().compareTo(other.planSignature()) < 0;
        }

        private List<LocalDateTime> joinedTimes() {
            return used.stream().mapToObj(candidates::get)
                    .map(MatchmakingCandidate::joinedTime)
                    .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                    .toList();
        }

        private int compareJoinedTimes(
                List<LocalDateTime> left,
                List<LocalDateTime> right) {
            for (int index = 0; index < left.size(); index++) {
                LocalDateTime leftTime = left.get(index);
                LocalDateTime rightTime = right.get(index);
                if (Objects.equals(leftTime, rightTime)) {
                    continue;
                }
                if (leftTime == null) {
                    return 1;
                }
                if (rightTime == null) {
                    return -1;
                }
                int comparison = Long.compare(
                        leftTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        rightTime.toInstant(ZoneOffset.UTC).toEpochMilli());
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }

        private String planSignature() {
            return groups.stream().map(FeasibleGroup::entrySignature).sorted()
                    .reduce((left, right) -> left + ";" + right).orElse("");
        }

        private void copyFrom(Plan source) {
            groups.clear();
            groups.addAll(source.groups);
            used.clear();
            used.or(source.used);
        }
    }
}
