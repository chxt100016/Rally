package com.rally.app.tournament;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.*;
import com.rally.tournament.bookingconfirm.activity.ConfirmBookingActivity;
import com.rally.tournament.bookingreject.activity.RejectAwaitBookingByBookerActivity;
import com.rally.tournament.bookingreject.activity.RejectAwaitBookingByOpponentActivity;
import com.rally.tournament.bookingreject.activity.RejectAwaitCourtBookerActivity;
import com.rally.tournament.bookingreject.activity.RejectAwaitScheduleConfirmActivity;
import com.rally.tournament.bookingreject.activity.RejectBookingOnConfirmActivity;
import com.rally.tournament.bookingreschedulerequest.activity.RequestRebookingActivity;
import com.rally.tournament.bookingsubmit.activity.SaveBookingActivity;
import com.rally.tournament.courtbookerselect.activity.SelectCourtBookerActivity;
import com.rally.tournament.resultconfirm.activity.ConfirmResultActivity;
import com.rally.tournament.resultreject.activity.RejectResultActivity;
import com.rally.tournament.resultsubmit.activity.SubmitResultActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentMatchAppService {

    private final SelectCourtBookerActivity selectCourtBookerActivity;
    private final SaveBookingActivity saveBookingActivity;
    private final RejectAwaitCourtBookerActivity rejectAwaitCourtBookerActivity;
    private final RejectAwaitBookingByBookerActivity rejectAwaitBookingByBookerActivity;
    private final RejectAwaitBookingByOpponentActivity rejectAwaitBookingByOpponentActivity;
    private final RejectAwaitScheduleConfirmActivity rejectAwaitScheduleConfirmActivity;
    private final ConfirmBookingActivity confirmBookingActivity;
    private final RequestRebookingActivity requestRebookingActivity;
    private final RejectBookingOnConfirmActivity rejectBookingOnConfirmActivity;
    private final SubmitResultActivity submitResultActivity;
    private final ConfirmResultActivity confirmResultActivity;
    private final RejectResultActivity rejectResultActivity;

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> selectCourtBooker(SelectCourtBookerCmd cmd) {
        String userId = UserContext.get();
        selectCourtBookerActivity.execute(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> submitBooking(SubmitBookingCmd cmd) {
        String userId = UserContext.get();
        String meetupId = saveBookingActivity.execute(cmd, userId);
        return Result.ok(meetupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitCourtBookerSelect(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        rejectAwaitCourtBookerActivity.execute(cmd.getMatchId(), userId, cmd.getRejectReason());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitBooking(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        rejectAwaitBookingByBookerActivity.execute(cmd.getMatchId(), userId, cmd.getRejectReason());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitBookingOpponent(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        rejectAwaitBookingByOpponentActivity.execute(cmd.getMatchId(), userId, cmd.getRejectReason());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectOnAwaitOpponentScheduleConfirm(RejectMatchCmd cmd) {
        String userId = UserContext.get();
        rejectAwaitScheduleConfirmActivity.execute(cmd.getMatchId(), userId, cmd.getRejectReason());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmSchedule(ScheduleConfirmCmd cmd) {
        String userId = UserContext.get();
        if (cmd.getConfirm()) {
            confirmBookingActivity.execute(cmd.getMatchId(), userId, LocalDateTime.now());
        } else if (cmd.getRebookReason() != null) {
            requestRebookingActivity.execute(cmd.getMatchId(), userId, cmd.getRebookReason());
        } else {
            rejectBookingOnConfirmActivity.execute(cmd.getMatchId(), userId, cmd.getRejectReason());
        }
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submitResult(SubmitResultCmd cmd) {
        String userId = UserContext.get();
        submitResultActivity.execute(
                cmd.getMatchId(), userId, cmd.getWinnerEntryNo(), LocalDateTime.now());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmResult(ResultConfirmCmd cmd) {
        String userId = UserContext.get();
        if (cmd.getConfirm()) {
            confirmResultActivity.execute(cmd.getMatchId(), userId, LocalDateTime.now());
        } else {
            rejectResultActivity.execute(cmd.getMatchId(), userId, cmd.getRejectReason());
        }
        return Result.ok();
    }

}
