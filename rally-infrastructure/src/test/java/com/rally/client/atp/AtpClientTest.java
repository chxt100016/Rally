package com.rally.client.atp;

import com.rally.client.atp.model.AtpAppDrawResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AtpClientTest {

    private final AtpClient atpClient = new AtpClient(new FlareSolverrClient());

    @Test
    public void testGetDraws() {
        AtpAppDrawResponse response = atpClient.getDraws("520", 2026);

        assertNotNull(response, "响应不应为 null");
        assertNotNull(response.getData(), "Data 不应为 null");

        AtpAppDrawResponse.Data data = response.getData();
        System.out.println("DrawSize: " + (data.getEvent() != null ? data.getEvent().getSglDrawSize() : "N/A"));

        if (data.getDraw() != null && !data.getDraw().isEmpty()) {
            AtpAppDrawResponse.DrawEntry first = data.getDraw().get(0);
            System.out.println("首位球员: " + first.getPlayerFirstName() + " " + first.getPlayerLastName());
        }

        if (data.getResults() != null && !data.getResults().isEmpty()) {
            AtpAppDrawResponse.RoundResult round = data.getResults().get(0);
            System.out.println("首轮: " + (round.getRound() != null ? round.getRound().getLongName() : "N/A")
                    + ", 场次数: " + (round.getMatches() != null ? round.getMatches().size() : 0));
        }
    }
}
