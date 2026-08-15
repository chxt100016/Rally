package com.rally.tour.client;

import com.rally.client.atp.model.AtpAppCompletedResponse;
import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.tour.model.Discipline;
import com.rally.tour.parser.DrawParams;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AtpEventResponseValidationTest {

    @Test
    public void liveResponseFromAnotherEventIsRejected() {
        AtpAppLiveMatchCollectClient parser = new AtpAppLiveMatchCollectClient();
        AtpAppLiveResponse response = liveResponse(2064, 2026);

        List<?> results = parser.buildDrawResult(
                response, new DrawParams("806", 2026, "WTA"), "LS", Discipline.SINGLES);

        assertTrue(results.isEmpty());
    }

    @Test
    public void liveResponseForRequestedEventIsAccepted() {
        AtpAppLiveMatchCollectClient parser = new AtpAppLiveMatchCollectClient();
        AtpAppLiveResponse response = liveResponse(806, 2026);

        List<?> results = parser.buildDrawResult(
                response, new DrawParams("806", 2026, "WTA"), "LS", Discipline.SINGLES);

        assertEquals(1, results.size());
    }

    @Test
    public void eventIdWithLeadingZeroMatchesNumericResponseId() {
        AtpAppLiveMatchCollectClient parser = new AtpAppLiveMatchCollectClient();
        AtpAppLiveResponse response = liveResponse(404, 2026);

        List<?> results = parser.buildDrawResult(
                response, new DrawParams("0404", 2026, "ATP"), "MS", Discipline.SINGLES);

        assertEquals(1, results.size());
    }

    @Test
    public void completedResponseFromAnotherYearIsRejected() {
        AtpCompletedMatchCollectClient parser = new AtpCompletedMatchCollectClient();
        AtpAppCompletedResponse response = completedResponse(806, 2025);

        assertTrue(parser.ls(response, new DrawParams("806", 2026, "WTA")).isEmpty());
    }

    private AtpAppLiveResponse liveResponse(int eventId, int year) {
        AtpAppLiveResponse.LiveMatch match = new AtpAppLiveResponse.LiveMatch();
        match.setMatchId(eventId == 404 ? "MS001" : "LS001");

        AtpAppLiveResponse.EventData data = new AtpAppLiveResponse.EventData();
        data.setEventId(eventId);
        data.setEventYear(year);
        data.setLiveMatches(List.of(match));

        AtpAppLiveResponse response = new AtpAppLiveResponse();
        response.setData(data);
        return response;
    }

    private AtpAppCompletedResponse completedResponse(int eventId, int year) {
        AtpAppCompletedResponse.Match match = new AtpAppCompletedResponse.Match();
        match.setMatchId("LS001");

        AtpAppCompletedResponse.DataWrapper data = new AtpAppCompletedResponse.DataWrapper();
        data.setEventId(eventId);
        data.setEventYear(year);
        data.setMatches(List.of(match));

        AtpAppCompletedResponse response = new AtpAppCompletedResponse();
        response.setData(data);
        return response;
    }
}
