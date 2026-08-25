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

    @PostMapping("/reject-on-await-court-booker-select")
    public Result<Void> rejectOnAwaitCourtBookerSelect(@Valid @RequestBody RejectMatchCmd cmd) {
        return tournamentMatchAppService.rejectOnAwaitCourtBookerSelect(cmd);
    }

    @PostMapping("/reject-on-await-booking")
    public Result<Void> rejectOnAwaitBooking(@Valid @RequestBody RejectMatchCmd cmd) {
        return tournamentMatchAppService.rejectOnAwaitBooking(cmd);
    }

    @PostMapping("/reject-on-await-booking-opponent")
    public Result<Void> rejectOnAwaitBookingOpponent(@Valid @RequestBody RejectMatchCmd cmd) {
        return tournamentMatchAppService.rejectOnAwaitBookingOpponent(cmd);
    }

    @PostMapping("/reject-on-await-opponent-schedule-confirm")
    public Result<Void> rejectOnAwaitOpponentScheduleConfirm(@Valid @RequestBody RejectMatchCmd cmd) {
        return tournamentMatchAppService.rejectOnAwaitOpponentScheduleConfirm(cmd);
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
