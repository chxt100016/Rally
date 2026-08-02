package com.rally.web.tournament;

import com.rally.app.tournament.TournamentMatchAppService;
import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tournament/match")
public class TournamentMatchController {

    @Resource
    private TournamentMatchAppService tournamentMatchAppService;

    @PostMapping("/court-booker")
    public Result<Void> selectCourtBooker(@Valid @RequestBody SelectCourtBookerCmd cmd) {
        return tournamentMatchAppService.selectCourtBooker(cmd);
    }

    @PostMapping("/book")
    public Result<String> submitBooking(@Valid @RequestBody SubmitBookingCmd cmd) {
        return tournamentMatchAppService.submitBooking(cmd);
    }

    @PostMapping("/schedule-confirm")
    public Result<Void> confirmSchedule(@Valid @RequestBody ScheduleConfirmCmd cmd) {
        return tournamentMatchAppService.confirmSchedule(cmd);
    }

    @PostMapping("/submit-result")
    public Result<Void> submitResult(@Valid @RequestBody SubmitResultCmd cmd) {
        return tournamentMatchAppService.submitResult(cmd);
    }

    @PostMapping("/result-confirm")
    public Result<Void> confirmResult(@Valid @RequestBody ResultConfirmCmd cmd) {
        return tournamentMatchAppService.confirmResult(cmd);
    }
}
