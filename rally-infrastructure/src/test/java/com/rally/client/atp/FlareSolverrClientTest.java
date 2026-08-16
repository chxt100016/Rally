package com.rally.client.atp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FlareSolverrClientTest {

    @Test
    public void extractsAndUnescapesJsonFromPreElement() {
        String response = "<html><body><pre>{\"CourtName\":\"P&amp;G Stadium Court\",\"Message\":\"A &lt; B\"}</pre></body></html>";

        assertEquals(
                "{\"CourtName\":\"P&G Stadium Court\",\"Message\":\"A < B\"}",
                FlareSolverrClient.extractJson(response)
        );
    }

    @Test
    public void leavesRawJsonUnchanged() {
        String response = "  {\"CourtName\":\"P&amp;G Stadium Court\"}  ";

        assertEquals(
                "{\"CourtName\":\"P&amp;G Stadium Court\"}",
                FlareSolverrClient.extractJson(response)
        );
    }

    @Test
    public void handlesNullResponse() {
        assertNull(FlareSolverrClient.extractJson(null));
    }
}
