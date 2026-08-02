package com.rally.domain.tournament.model;

import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.tournament.enums.CourtAbilityEnum;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 匹配算法使用的参赛队；单打为一名成员，双打为共享同一 entryNo 的两名成员。 */
@Getter
public class TournamentMatchTeam {
    private final Integer entryNo;
    private final List<TournamentEntryData> entries;
    private final Set<String> preferredDistricts;
    /** 队内所有成员均可参加的时间段；双打取两名成员时间的交集。 */
    private final Set<String> availableTimes;
    private final String genderSignature;
    private final LocalDateTime joinedTime;
    /** 队伍中实际可订场的成员数；双打按成员而非 entryNo 计数。 */
    private final int canBookMemberCount;

    public TournamentMatchTeam(Integer entryNo, List<TournamentEntryData> entries, Set<String> preferredDistricts,
                               List<GenderEnum> genders, LocalDateTime joinedTime) {
        this.entryNo = entryNo;
        this.entries = entries;
        this.preferredDistricts = preferredDistricts;
        this.availableTimes = commonAvailableTimes(entries);
        this.genderSignature = genders.stream().map(gender -> gender == null ? "UNKNOWN" : gender.name()).sorted()
                .reduce((a, b) -> a + "|" + b).orElse("UNKNOWN");
        this.joinedTime = joinedTime;
        this.canBookMemberCount = (int) entries.stream()
                .filter(entry -> entry.getCourtAbility() == CourtAbilityEnum.CAN_BOOK).count();
    }

    private Set<String> commonAvailableTimes(List<TournamentEntryData> entries) {
        Set<String> common = null;
        for (TournamentEntryData entry : entries) {
            Set<String> memberTimes = entry.getAvailableTimes() == null ? Set.of() : Set.copyOf(entry.getAvailableTimes());
            if (common == null) {
                common = new java.util.HashSet<>(memberTimes);
            } else {
                common.retainAll(memberTimes);
            }
        }
        return common == null ? Set.of() : Set.copyOf(common);
    }
}
