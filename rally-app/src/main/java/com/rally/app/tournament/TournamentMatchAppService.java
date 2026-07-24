package com.rally.app.tournament;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.enums.ResultRejectReasonEnum;
import com.rally.domain.tournament.enums.RebookReasonEnum;
import com.rally.domain.tournament.enums.ScheduleRejectReasonEnum;
import com.rally.domain.tournament.model.*;
import com.rally.domain.tournament.service.TournamentMatchFlowService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentMatchAppService {

    private final TournamentMatchFlowService matchFlowService;

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> selectCourtBooker(SelectCourtBookerCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.selectCourtBooker(cmd.getMatchId(), userId);
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> submitBooking(SubmitBookingCmd cmd) {
        String userId = UserContext.get();
        String meetupId = matchFlowService.submitBooking(cmd, userId);
        return Result.ok(meetupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmSchedule(ScheduleConfirmCmd cmd) {
        String userId = UserContext.get();
        ScheduleRejectReasonEnum rejectReason = cmd.getRejectReason() != null ? ScheduleRejectReasonEnum.valueOf(cmd.getRejectReason()) : null;
        RebookReasonEnum rebookReason = cmd.getRebookReason() != null ? RebookReasonEnum.valueOf(cmd.getRebookReason()) : null;
        matchFlowService.handleScheduleConfirm(cmd.getMatchId(), userId, cmd.getConfirm(), rejectReason, cmd.getRejectReasonText(), rebookReason, cmd.getRebookReasonText());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submitResult(SubmitResultCmd cmd) {
        String userId = UserContext.get();
        matchFlowService.submitResult(cmd.getMatchId(), userId, cmd.getWinnerUserIds());
        return Result.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmResult(ResultConfirmCmd cmd) {
        String userId = UserContext.get();
        ResultRejectReasonEnum rejectReason = cmd.getRejectReason() != null ? ResultRejectReasonEnum.valueOf(cmd.getRejectReason()) : null;
        matchFlowService.handleResultConfirm(cmd.getMatchId(), userId, cmd.getConfirm(), rejectReason, cmd.getRejectReasonText());
        return Result.ok();
    }
}
