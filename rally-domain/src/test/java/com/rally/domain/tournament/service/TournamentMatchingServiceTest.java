package com.rally.domain.tournament.service;

import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatchTeam;
import com.rally.domain.tournament.enums.CourtAbilityEnum;
import com.rally.domain.user.enums.GenderEnum;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TournamentMatchingServiceTest {
    private final TournamentMatchingService service = new TournamentMatchingService();

    @Test
    public void shouldChooseGlobalDistrictPairingInsteadOfGreedyPairing() {
        // abc 与 c 配对后，两个 ab 才仍能配对；若 abc 先取 ab，c 就会被永久落单。
        List<MatchGroup> groups = service.group(List.of(
                team(1, List.of("a", "b", "c"), GenderEnum.MALE, 1),
                team(2, List.of("a", "b"), GenderEnum.MALE, 2),
                team(3, List.of("c"), GenderEnum.MALE, 3),
                team(4, List.of("a", "b"), GenderEnum.MALE, 4)), 2, Set.of());

        assertEquals(2, groups.size());
        assertTrue(groups.stream().anyMatch(group -> entryNos(group).equals(Set.of(1, 3))));
        assertTrue(groups.stream().anyMatch(group -> entryNos(group).equals(Set.of(2, 4))));
    }

    @Test
    public void shouldUsePlayedPairOnlyAsLastPairFallback() {
        List<MatchGroup> groups = service.group(List.of(
                team(1, List.of("a"), GenderEnum.MALE, 1),
                team(2, List.of("a"), GenderEnum.MALE, 2)), 2, Set.of("1|2"));

        assertEquals(1, groups.size());
        assertEquals(Set.of(1, 2), entryNos(groups.get(0)));
    }

    @Test
    public void shouldPreferSameGenderWhenCoverageIsEqual() {
        List<MatchGroup> groups = service.group(List.of(
                team(1, List.of("a"), GenderEnum.MALE, 1),
                team(2, List.of("a"), GenderEnum.FEMALE, 2),
                team(3, List.of("a"), GenderEnum.MALE, 3)), 2, Set.of());

        assertEquals(Set.of(1, 3), entryNos(groups.get(0)));
    }

    @Test
    public void shouldPreferExactlyOneCourtBookerBeforeGender() {
        TournamentMatchTeam canBook = team(1, List.of("a"), GenderEnum.MALE, CourtAbilityEnum.CAN_BOOK, 1);
        TournamentMatchTeam cannotBookFemale = team(2, List.of("a"), GenderEnum.FEMALE, 2);
        TournamentMatchTeam secondCanBookMale = team(3, List.of("a"), GenderEnum.MALE, CourtAbilityEnum.CAN_BOOK, 3);

        List<MatchGroup> groups = service.group(List.of(canBook, cannotBookFemale, secondCanBookMale), 2, Set.of());

        // 1+2 有唯一订场人，应优先于性别相同但两人都能订场的 1+3。
        assertEquals(Set.of(1, 2), entryNos(groups.get(0)));
    }

    @Test
    public void shouldRequireOneCommonAvailableTimeForEntireGroup() {
        TournamentMatchTeam first = team(1, List.of("a"), GenderEnum.MALE, 1, List.of("morning", "afternoon"));
        TournamentMatchTeam second = team(2, List.of("a"), GenderEnum.MALE, 2, List.of("afternoon", "evening"));
        TournamentMatchTeam third = team(3, List.of("a"), GenderEnum.MALE, 3, List.of("morning", "evening"));

        assertTrue(service.group(List.of(first, second, third), 3, Set.of()).isEmpty());
    }

    private TournamentMatchTeam team(int entryNo, List<String> districts, GenderEnum gender, int order) {
        return team(entryNo, districts, gender, CourtAbilityEnum.CANNOT_BOOK, order, List.of("any"));
    }

    private TournamentMatchTeam team(int entryNo, List<String> districts, GenderEnum gender, CourtAbilityEnum courtAbility, int order) {
        return team(entryNo, districts, gender, courtAbility, order, List.of("any"));
    }

    private TournamentMatchTeam team(int entryNo, List<String> districts, GenderEnum gender, int order, List<String> availableTimes) {
        return team(entryNo, districts, gender, CourtAbilityEnum.CANNOT_BOOK, order, availableTimes);
    }

    private TournamentMatchTeam team(int entryNo, List<String> districts, GenderEnum gender, CourtAbilityEnum courtAbility, int order, List<String> availableTimes) {
        TournamentEntryData entry = new TournamentEntryData();
        entry.setEntryNo(entryNo);
        entry.setUserId("user-" + entryNo);
        entry.setPreferredDistricts(districts);
        entry.setCourtAbility(courtAbility);
        entry.setAvailableTimes(availableTimes);
        entry.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(order));
        return new TournamentMatchTeam(entryNo, List.of(entry), Set.copyOf(districts), List.of(gender), entry.getCreateTime());
    }

    private Set<Integer> entryNos(MatchGroup group) {
        return group.getMembers().stream().map(TournamentEntryData::getEntryNo).collect(java.util.stream.Collectors.toSet());
    }
}
