package com.rally.tournament;

import com.rally.domain.tournament.model.MatchParticipantDTO;
import com.rally.domain.tournament.model.MyCurrentMatchDTO;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TournamentDetailAppServiceTest {

    private final TournamentDetailAppService tournamentDetailAppService = new TournamentDetailAppService(null, null, null, null, null);

    @Test
    public void shouldOnlyFillPhonesForParticipantsInOpponentEntry() {
        TournamentDetailDTO detail = detail(1, participant("me", 1), participant("partner", 1), participant("opponent-1", 2), participant("opponent-2", 2));
        Map<String, UserProfile> profiles = Map.of(
                "me", userProfile("13000000000"),
                "partner", userProfile("13100000000"),
                "opponent-1", userProfile("13200000000"),
                "opponent-2", userProfile("13300000000"));

        tournamentDetailAppService.fillOpponentPhones(detail, "me", profiles);

        assertNull(detail.getMyCurrentMatch().getParticipants().get(0).getPhone());
        assertNull(detail.getMyCurrentMatch().getParticipants().get(1).getPhone());
        assertEquals("13200000000", detail.getMyCurrentMatch().getParticipants().get(2).getPhone());
        assertEquals("13300000000", detail.getMyCurrentMatch().getParticipants().get(3).getPhone());
    }

    @Test
    public void shouldNotFillAnyPhoneWhenCurrentEntryCannotBeConfirmed() {
        TournamentDetailDTO detail = detail(1, participant("me", 2), participant("opponent", 3));

        tournamentDetailAppService.fillOpponentPhones(detail, "me", Map.of("opponent", userProfile("13200000000")));

        assertNull(detail.getMyCurrentMatch().getParticipants().get(0).getPhone());
        assertNull(detail.getMyCurrentMatch().getParticipants().get(1).getPhone());
    }

    private TournamentDetailDTO detail(Integer entryNo, MatchParticipantDTO... participants) {
        TournamentEntryDTO entry = new TournamentEntryDTO();
        entry.setEntryNo(entryNo);
        MyCurrentMatchDTO match = new MyCurrentMatchDTO();
        match.setParticipants(List.of(participants));
        TournamentDetailDTO detail = new TournamentDetailDTO();
        detail.setMyEntry(entry);
        detail.setMyCurrentMatch(match);
        return detail;
    }

    private MatchParticipantDTO participant(String userId, Integer entryNo) {
        MatchParticipantDTO participant = new MatchParticipantDTO();
        participant.setUserId(userId);
        participant.setEntryNo(entryNo);
        return participant;
    }

    private UserProfile userProfile(String phone) {
        UserData user = new UserData();
        user.setPhone(phone);
        return UserProfile.create(user, null);
    }
}
