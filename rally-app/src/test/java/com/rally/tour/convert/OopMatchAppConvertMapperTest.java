package com.rally.tour.convert;

import com.alibaba.fastjson2.JSON;
import com.rally.client.tourtv.model.AtpOopResponse;
import com.rally.tour.model.Match;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OopMatchAppConvertMapperTest {

    @Test
    public void scheduledMatchReadsPlayersFromTeamsInMatch() {
        String json = """
                {
                  "MatchId":"MS001",
                  "TeamsInMatch":[
                    {"TeamNumber":1,"Players":[{"PlayerId":"S0S1","OrderInTeam":1,"FirstName":"Ben","LastName":"Shelton"}]},
                    {"TeamNumber":2,"Players":[{"PlayerId":"N0AE","OrderInTeam":1,"FirstName":"Brandon","LastName":"Nakashima"}]}
                  ],
                  "Round":{"RoundName":"Final"},
                  "Status":"Scheduled",
                  "TournamentId":421,
                  "TournamentYear":2026
                }
                """;

        Match match = OopMatchAppConvertMapper.INSTANCE.toMatch(
                JSON.parseObject(json, AtpOopResponse.MatchDetail.class));

        assertEquals("S0S1", match.getPlayer1Id());
        assertEquals("N0AE", match.getPlayer2Id());
        assertEquals("Ben Shelton", match.getPlayerName1());
        assertEquals("Brandon Nakashima", match.getPlayerName2());
        assertEquals("F", match.getRoundName());
    }

    @Test
    public void legacyPlayerTeamsRemainSupported() {
        String json = """
                {
                  "MatchId":"MS009",
                  "PlayerTeam1":{"PlayerId":"D0FJ","PlayerFirstNameFull":"Luciano","PlayerLastName":"Darderi"},
                  "PlayerTeam2":{"PlayerId":"C0AU","PlayerFirstNameFull":"Francisco","PlayerLastName":"Cerundolo"},
                  "Round":{"LongName":"Round of 16"}
                }
                """;

        Match match = OopMatchAppConvertMapper.INSTANCE.toMatch(
                JSON.parseObject(json, AtpOopResponse.MatchDetail.class));

        assertEquals("D0FJ", match.getPlayer1Id());
        assertEquals("C0AU", match.getPlayer2Id());
        assertEquals("Luciano Darderi", match.getPlayerName1());
        assertEquals("Francisco Cerundolo", match.getPlayerName2());
        assertEquals("R16", match.getRoundName());
    }
}
