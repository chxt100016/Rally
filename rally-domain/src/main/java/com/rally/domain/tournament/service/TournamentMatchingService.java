package com.rally.domain.tournament.service;

import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.TournamentMatchTeam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 当前轮次的全局匹配算法。
 * <p>
 * 以 entryNo（单打为选手、双打为固定队伍）为单位，在地区和可打时间交集这两个硬约束下枚举可行分组，
 * 再选出覆盖队伍数最多的组合。因此不会出现贪心先配错一组而让后续本可匹配的队伍落单的情况。
 * 在覆盖数量相同的方案中，依次优先恰好一名可订场成员、至少一名可订场成员、同性别构成和更早报名的队伍。
 */
@Service
public class TournamentMatchingService {

    /**
     * 为一批队伍计算自动分组。历史对阵是当前赛事、当前轮次中已出现过的 entryNo 对；
     * 除非严格匹配后只剩恰好一组队伍，否则历史对阵不会被再次安排。
     */
    public List<MatchGroup> group(List<TournamentMatchTeam> teams, int groupSize, Set<String> playedPairs) {
        if (teams.size() < groupSize) {
            return List.of();
        }
        List<CandidateGroup> strictGroups = buildCandidateGroups(teams, groupSize, playedPairs, false);
        Plan plan = selectBest(teams.size(), strictGroups);

        // 历史对阵仅作为最后一组的兜底，不能为了多配一场而牺牲其他可选组合。
        List<Integer> leftover = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            if (!plan.used.contains(i)) {
                leftover.add(i);
            }
        }
        if (leftover.size() == groupSize && compatibleDistricts(leftover, teams)) {
            plan.groups.add(new CandidateGroup(leftover, teams, true));
        }
        return plan.groups.stream().map(group -> new MatchGroup(group.indices.stream()
                        .flatMap(index -> teams.get(index).getEntries().stream()).toList()))
                .toList();
    }

    private List<CandidateGroup> buildCandidateGroups(List<TournamentMatchTeam> teams, int groupSize,
                                                        Set<String> playedPairs, boolean allowPlayed) {
        List<CandidateGroup> groups = new ArrayList<>();
        enumerateGroups(teams, groupSize, playedPairs, allowPlayed, 0, new ArrayList<>(), groups);
        return groups;
    }

    private void enumerateGroups(List<TournamentMatchTeam> teams, int groupSize, Set<String> playedPairs,
                                 boolean allowPlayed, int start, List<Integer> selected, List<CandidateGroup> groups) {
        if (selected.size() == groupSize) {
            groups.add(new CandidateGroup(selected, teams, false));
            return;
        }
        for (int i = start; i < teams.size(); i++) {
            if (!canJoin(i, selected, teams, playedPairs, allowPlayed)) {
                continue;
            }
            selected.add(i);
            enumerateGroups(teams, groupSize, playedPairs, allowPlayed, i + 1, selected, groups);
            selected.remove(selected.size() - 1);
        }
    }

    private boolean canJoin(int candidate, List<Integer> selected, List<TournamentMatchTeam> teams,
                            Set<String> playedPairs, boolean allowPlayed) {
        if (!hasCommonAvailableTime(candidate, selected, teams)) {
            return false;
        }
        for (Integer member : selected) {
            if (!districtsIntersect(teams.get(member).getPreferredDistricts(), teams.get(candidate).getPreferredDistricts())) {
                return false;
            }
            if (!allowPlayed && playedPairs.contains(pairKey(teams.get(member).getEntryNo(), teams.get(candidate).getEntryNo()))) {
                return false;
            }
        }
        return true;
    }

    private boolean compatibleDistricts(List<Integer> indices, List<TournamentMatchTeam> teams) {
        if (!hasCommonAvailableTime(indices, teams)) {
            return false;
        }
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                if (!districtsIntersect(teams.get(indices.get(i)).getPreferredDistricts(), teams.get(indices.get(j)).getPreferredDistricts())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 所有队伍必须存在同一个可打时间段；这比两两时间交集更严格，适用于三人及以上资格赛。 */
    private boolean hasCommonAvailableTime(int candidate, List<Integer> selected, List<TournamentMatchTeam> teams) {
        List<Integer> indices = new ArrayList<>(selected);
        indices.add(candidate);
        return hasCommonAvailableTime(indices, teams);
    }

    private boolean hasCommonAvailableTime(List<Integer> indices, List<TournamentMatchTeam> teams) {
        Set<String> common = null;
        for (Integer index : indices) {
            Set<String> times = teams.get(index).getAvailableTimes();
            if (times.isEmpty()) {
                return false;
            }
            if (common == null) {
                common = new HashSet<>(times);
            } else {
                common.retainAll(times);
            }
            if (common.isEmpty()) {
                return false;
            }
        }
        return common != null && !common.isEmpty();
    }

    private boolean districtsIntersect(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return true;
        }
        return left.stream().anyMatch(right::contains);
    }

    private Plan selectBest(int teamCount, List<CandidateGroup> groups) {
        Plan best = new Plan();
        search(0, teamCount, groups, new Plan(), best);
        return best;
    }

    private void search(int index, int teamCount, List<CandidateGroup> candidates, Plan current, Plan best) {
        if (index >= teamCount) {
            if (current.betterThan(best)) {
                best.copyFrom(current);
            }
            return;
        }
        if (current.used.contains(index)) {
            search(index + 1, teamCount, candidates, current, best);
            return;
        }
        // 此队本次落单。
        search(index + 1, teamCount, candidates, current, best);
        for (CandidateGroup group : candidates) {
            if (!group.indices.contains(index) || !current.disjoint(group)) {
                continue;
            }
            current.add(group);
            search(index + 1, teamCount, candidates, current, best);
            current.remove(group);
        }
    }

    private String pairKey(Integer a, Integer b) {
        return a < b ? a + "|" + b : b + "|" + a;
    }

    private static class CandidateGroup {
        private final List<Integer> indices;
        private final int canBookMemberCount;
        private final boolean exactlyOneCanBook;
        private final boolean hasCanBook;
        private final boolean sameGender;
        private final long joinedOrder;

        private CandidateGroup(List<Integer> indices, List<TournamentMatchTeam> teams, boolean repeated) {
            this.indices = List.copyOf(indices);
            this.canBookMemberCount = indices.stream().mapToInt(i -> teams.get(i).getCanBookMemberCount()).sum();
            this.exactlyOneCanBook = canBookMemberCount == 1;
            this.hasCanBook = canBookMemberCount > 0;
            this.sameGender = indices.stream().map(i -> teams.get(i).getGenderSignature()).distinct().count() == 1;
            this.joinedOrder = indices.stream().map(i -> teams.get(i).getJoinedTime()).filter(java.util.Objects::nonNull)
                    .mapToLong(time -> time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()).sum();
        }
    }

    private static class Plan {
        private final List<CandidateGroup> groups = new ArrayList<>();
        private final Set<Integer> used = new HashSet<>();

        private boolean disjoint(CandidateGroup group) {
            return group.indices.stream().noneMatch(used::contains);
        }

        private void add(CandidateGroup group) {
            groups.add(group);
            used.addAll(group.indices);
        }

        private void remove(CandidateGroup group) {
            groups.remove(groups.size() - 1);
            used.removeAll(group.indices);
        }

        private boolean betterThan(Plan other) {
            if (used.size() != other.used.size()) return used.size() > other.used.size();
            long exactlyOneCanBook = groups.stream().filter(g -> g.exactlyOneCanBook).count();
            long otherExactlyOneCanBook = other.groups.stream().filter(g -> g.exactlyOneCanBook).count();
            if (exactlyOneCanBook != otherExactlyOneCanBook) return exactlyOneCanBook > otherExactlyOneCanBook;
            long hasCanBook = groups.stream().filter(g -> g.hasCanBook).count();
            long otherHasCanBook = other.groups.stream().filter(g -> g.hasCanBook).count();
            if (hasCanBook != otherHasCanBook) return hasCanBook > otherHasCanBook;
            long sameGender = groups.stream().filter(g -> g.sameGender).count();
            long otherSameGender = other.groups.stream().filter(g -> g.sameGender).count();
            if (sameGender != otherSameGender) return sameGender > otherSameGender;
            long joined = groups.stream().mapToLong(g -> g.joinedOrder).sum();
            long otherJoined = other.groups.stream().mapToLong(g -> g.joinedOrder).sum();
            return joined < otherJoined;
        }

        private void copyFrom(Plan source) {
            groups.clear();
            groups.addAll(source.groups);
            used.clear();
            used.addAll(source.used);
        }
    }
}
