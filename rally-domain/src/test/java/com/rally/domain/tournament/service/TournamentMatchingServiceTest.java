package com.rally.domain.tournament.service;

import com.rally.domain.tournament.enums.CourtAbilityEnum;
import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.TournamentEntryData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TournamentMatchingServiceTest {

    private final TournamentMatchingService service = new TournamentMatchingService();

    private TournamentEntryData entry(String userId, List<String> districts) {
        TournamentEntryData data = new TournamentEntryData();
        data.setUserId(userId);
        data.setCourtAbility(CourtAbilityEnum.CANNOT_BOOK);
        data.setPreferredDistricts(districts);
        return data;
    }

    private static final RejectHistoryLookup NO_REJECTIONS = (a, b) -> false;

    @Test
    public void testDistrictIntersectionRequired() {
        // A、B 区域有交集，C 与两者都无交集，理应只成一组 A+B，C 落单
        TournamentEntryData a = entry("A", List.of("朝阳", "海淀"));
        TournamentEntryData b = entry("B", List.of("海淀", "西城"));
        TournamentEntryData c = entry("C", List.of("通州"));

        List<MatchGroup> groups = service.group(List.of(a, b, c), 2, NO_REJECTIONS);

        assertEquals(1, groups.size());
        List<String> memberIds = userIds(groups.get(0));
        assertTrue(memberIds.contains("A"));
        assertTrue(memberIds.contains("B"));
        assertFalse(memberIds.contains("C"));
    }

    @Test
    public void testRejectHistoryAvoided() {
        // A、B 互相拒绝过，A、C 未拒绝过；期望优先把 A 与 C 分为一组，B 落单
        TournamentEntryData a = entry("A", null);
        TournamentEntryData b = entry("B", null);
        TournamentEntryData c = entry("C", null);

        RejectHistoryLookup rejectHistory = (u1, u2) ->
                (u1.equals("A") && u2.equals("B")) || (u1.equals("B") && u2.equals("A"));

        List<MatchGroup> groups = service.group(List.of(a, b, c), 2, rejectHistory);

        assertEquals(1, groups.size());
        List<String> memberIds = userIds(groups.get(0));
        assertFalse(memberIds.contains("A") && memberIds.contains("B"));
    }

    @Test
    public void testRejectHistoryFallbackWhenCannotFillGroup() {
        // 只剩 A、B 两人且互相拒绝过，无法凑齐第三人，兜底强制分组
        TournamentEntryData a = entry("A", null);
        TournamentEntryData b = entry("B", null);

        RejectHistoryLookup rejectHistory = (u1, u2) -> true;

        List<MatchGroup> groups = service.group(List.of(a, b), 2, rejectHistory);

        assertEquals(1, groups.size());
        List<String> memberIds = userIds(groups.get(0));
        assertTrue(memberIds.contains("A"));
        assertTrue(memberIds.contains("B"));
    }

    @Test
    public void testLeftoverNotIncludedInResult() {
        // 单人无法成组（groupSize=2），不应出现在任何分组结果里
        TournamentEntryData a = entry("A", null);

        List<MatchGroup> groups = service.group(List.of(a), 2, NO_REJECTIONS);

        assertTrue(groups.isEmpty());
    }

    @Test
    public void testGroupSizeThreeExactMatch() {
        TournamentEntryData a = entry("A", null);
        TournamentEntryData b = entry("B", null);
        TournamentEntryData c = entry("C", null);

        List<MatchGroup> groups = service.group(List.of(a, b, c), 3, NO_REJECTIONS);

        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).getMembers().size());
    }

    private List<String> userIds(MatchGroup group) {
        List<String> ids = new ArrayList<>();
        group.getMembers().forEach(m -> ids.add(m.getUserId()));
        return ids;
    }
}
