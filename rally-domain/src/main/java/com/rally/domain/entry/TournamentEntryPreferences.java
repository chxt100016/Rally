package com.rally.domain.tournament.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 必须整组替换的匹配偏好。 */
public record TournamentEntryPreferences(
        List<String> preferredDistricts,
        TournamentEntryCourtAbility courtAbility,
        List<String> availableTimes) {

    public TournamentEntryPreferences {
        preferredDistricts = checkedItems(preferredDistricts, "活动地区");
        availableTimes = checkedItems(availableTimes, "可比赛时间");
        if (courtAbility == null) {
            throw invalid("订场能力不能为空");
        }
    }

    private static List<String> checkedItems(List<String> values, String label) {
        if (values == null || values.isEmpty()) {
            throw invalid(label + "至少需要一项");
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static TournamentEntryDomainException invalid(String message) {
        return new TournamentEntryDomainException(
                TournamentEntry.TOURNAMENT_ENTRY_PREFERENCE_INVALID, message);
    }
}
