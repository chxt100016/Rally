package com.rally.domain.tournament.service;

import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.TournamentEntryData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 匹配算法领域服务（纯算法，无副作用，不依赖 Repository）
 * <p>
 * 输入：候选人列表 + 每组人数 + 拒绝历史查询；输出：分组结果。
 * 步骤：活动区域交集判断 → 排除互相拒绝过的组合（兜底强制凑组）→ 随机分组 → 落单人放回候选池（不出现在结果中）。
 */
@Service
public class TournamentMatchingService {

    public List<MatchGroup> group(List<TournamentEntryData> candidates, int groupSize, RejectHistoryLookup rejectHistory) {
        List<TournamentEntryData> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool);

        List<MatchGroup> groups = new ArrayList<>();

        // 第一轮：严格避让互相拒绝过的组合
        List<TournamentEntryData> leftover = greedyGroup(pool, groupSize, rejectHistory, groups);

        // 第二轮兜底：对第一轮凑不齐的人，放宽拒绝限制（仍要求活动区域有交集）强制凑组
        List<TournamentEntryData> stillLeftover = greedyGroup(leftover, groupSize, (a, b) -> false, groups);

        // stillLeftover 中的人本轮无法成组，放回候选池（不写入 groups，等下次匹配 Job 再处理）
        return groups;
    }

    /**
     * 贪心分组：取一人作种子，在剩余候选中挑选与其活动区域有交集、且未被拒绝历史排除的人凑够 groupSize；
     * 凑不齐则种子放入落单列表，继续处理下一个种子。返回本轮未能成组的落单人列表。
     */
    private List<TournamentEntryData> greedyGroup(List<TournamentEntryData> pool, int groupSize, RejectHistoryLookup rejectHistory, List<MatchGroup> groups) {
        List<TournamentEntryData> remaining = new ArrayList<>(pool);
        List<TournamentEntryData> leftover = new ArrayList<>();

        while (!remaining.isEmpty()) {
            TournamentEntryData seed = remaining.remove(0);
            List<TournamentEntryData> members = new ArrayList<>();
            members.add(seed);

            List<TournamentEntryData> candidatesForSeed = new ArrayList<>(remaining);
            for (TournamentEntryData candidate : candidatesForSeed) {
                if (members.size() >= groupSize) {
                    break;
                }
                if (!hasDistrictIntersection(members, candidate)) {
                    continue;
                }
                if (hasRejectedAnyMember(members, candidate, rejectHistory)) {
                    continue;
                }
                members.add(candidate);
                remaining.remove(candidate);
            }

            if (members.size() == groupSize) {
                groups.add(new MatchGroup(members));
            } else {
                leftover.addAll(members);
            }
        }

        return leftover;
    }

    private boolean hasDistrictIntersection(List<TournamentEntryData> members, TournamentEntryData candidate) {
        for (TournamentEntryData member : members) {
            if (!districtsIntersect(member.getPreferredDistricts(), candidate.getPreferredDistricts())) {
                return false;
            }
        }
        return true;
    }

    /** 任一方未设置活动区域偏好视为无限制，与任何人都有交集 */
    private boolean districtsIntersect(List<String> a, List<String> b) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty()) {
            return true;
        }
        Set<String> setA = new HashSet<>(a);
        for (String district : b) {
            if (setA.contains(district)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRejectedAnyMember(List<TournamentEntryData> members, TournamentEntryData candidate, RejectHistoryLookup rejectHistory) {
        for (TournamentEntryData member : members) {
            if (rejectHistory.hasRejected(member.getUserId(), candidate.getUserId())) {
                return true;
            }
        }
        return false;
    }
}
