package com.rally.domain.tournament.entry;

import java.util.List;

/** C1 创建资格赛报名输入；搭档的反向绑定由调用活动在同一事务中协调。 */
public record CreateTournamentEntryCommand(
        String tournamentId,
        String userId,
        String partnerId,
        int entryNo,
        List<String> preferredDistricts,
        TournamentEntryCourtAbility courtAbility,
        List<String> availableTimes,
        boolean partnerAlreadyPairedWithOther,
        Integer registeredPartnerEntryNo) {
}
