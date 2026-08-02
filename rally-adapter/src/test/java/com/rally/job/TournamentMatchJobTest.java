package com.rally.job;

import com.rally.tournament.TournamentAdminAppService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TournamentMatchJobTest {

    @Test
    public void runDelegatesToAdminAppService() {
        RecordingTournamentAdminAppService appService = new RecordingTournamentAdminAppService();
        TournamentMatchJob job = new TournamentMatchJob(appService);

        job.run();

        assertEquals(1, appService.runCount);
    }

    private static class RecordingTournamentAdminAppService extends TournamentAdminAppService {

        private int runCount;

        private RecordingTournamentAdminAppService() {
            super(null, null, null, null);
        }

        @Override
        public void runTournamentMatch() {
            runCount++;
        }
    }
}
