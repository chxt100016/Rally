package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.*;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TournamentMatchTest {

    @Test
    public void testCreateFromGroup_singleBooker() {
        List<TournamentEntryData> candidates = new ArrayList<>();
        TournamentEntryData entry1 = new TournamentEntryData();
        entry1.setUserId("user1");
        entry1.setCourtAbility(CourtAbilityEnum.CAN_BOOK);
        TournamentEntryData entry2 = new TournamentEntryData();
        entry2.setUserId("user2");
        entry2.setCourtAbility(CourtAbilityEnum.CANNOT_BOOK);
        candidates.add(entry1);
        candidates.add(entry2);

        TournamentMatch match = TournamentMatch.createFromGroup("tournament1", 1, TournamentRoundEnum.QUALIFIER, 2, candidates);

        Assert.assertEquals(TournamentMatchStatusEnum.BOOKING, match.getData().getStatus());
        Assert.assertEquals("user1", match.getData().getCourtBookerId());
        Assert.assertNotNull(match.getData().getCourtBookerSelectedTime());
        Assert.assertEquals(2, match.getParticipants().size());
    }

    @Test
    public void testCreateFromGroup_multipleBookers() {
        List<TournamentEntryData> candidates = new ArrayList<>();
        TournamentEntryData entry1 = new TournamentEntryData();
        entry1.setUserId("user1");
        entry1.setCourtAbility(CourtAbilityEnum.CAN_BOOK);
        TournamentEntryData entry2 = new TournamentEntryData();
        entry2.setUserId("user2");
        entry2.setCourtAbility(CourtAbilityEnum.CAN_BOOK);
        candidates.add(entry1);
        candidates.add(entry2);

        TournamentMatch match = TournamentMatch.createFromGroup("tournament1", 1, TournamentRoundEnum.QUALIFIER, 2, candidates);

        Assert.assertEquals(TournamentMatchStatusEnum.MATCHED, match.getData().getStatus());
        Assert.assertNull(match.getData().getCourtBookerId());
    }

    @Test
    public void testSelectCourtBooker() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.MATCHED);
        List<MatchParticipantData> participants = new ArrayList<>();
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        participants.add(p1);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.selectCourtBooker("user1");

        Assert.assertEquals(TournamentMatchStatusEnum.BOOKING, match.getData().getStatus());
        Assert.assertEquals("user1", match.getData().getCourtBookerId());
        Assert.assertNotNull(match.getData().getCourtBookerSelectedTime());
    }

    @Test
    public void testGiveUpCourtBooker() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.BOOKING);
        data.setCourtBookerId("user1");
        List<MatchParticipantData> participants = new ArrayList<>();

        TournamentMatch match = new TournamentMatch(data, participants);
        match.giveUpCourtBooker("user1");

        Assert.assertEquals(TournamentMatchStatusEnum.MATCHED, match.getData().getStatus());
        Assert.assertNull(match.getData().getCourtBookerId());
    }

    @Test
    public void testSubmitBooking() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.BOOKING);
        data.setCourtBookerId("user1");
        List<MatchParticipantData> participants = Arrays.asList(new MatchParticipantData(), new MatchParticipantData());

        TournamentMatch match = new TournamentMatch(data, participants);
        match.submitBooking("user1");

        Assert.assertEquals(TournamentMatchStatusEnum.SCHEDULED, match.getData().getStatus());
        Assert.assertNotNull(match.getData().getScheduleSubmittedTime());
    }

    @Test
    public void testConfirmSchedule_AllConfirm() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.SCHEDULED);
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        p1.setConfirmStatus(ConfirmStatusEnum.CONFIRMED);
        MatchParticipantData p2 = new MatchParticipantData();
        p2.setUserId("user2");
        p2.setConfirmStatus(ConfirmStatusEnum.PENDING);
        List<MatchParticipantData> participants = Arrays.asList(p1, p2);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.confirmSchedule("user2", true, null, null, 1, 1, TournamentEntryStageEnum.QUALIFY, 0);

        Assert.assertEquals(TournamentMatchStatusEnum.PENDING_PLAY, match.getData().getStatus());
        Assert.assertEquals(ConfirmStatusEnum.CONFIRMED, p2.getConfirmStatus());
    }

    @Test
    public void testConfirmSchedule_AnyReject() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.SCHEDULED);
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        p1.setConfirmStatus(ConfirmStatusEnum.PENDING);
        List<MatchParticipantData> participants = Arrays.asList(p1);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.confirmSchedule("user1", false, ScheduleRejectReasonEnum.DONT_WANT_PLAY, null, 1, 1, TournamentEntryStageEnum.QUALIFY, 0);

        Assert.assertEquals(TournamentMatchStatusEnum.REJECTED, match.getData().getStatus());
        Assert.assertEquals(ConfirmStatusEnum.REJECTED, p1.getConfirmStatus());
    }

    @Test
    public void testConfirmSchedule_Rebook() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.SCHEDULED);
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        p1.setConfirmStatus(ConfirmStatusEnum.PENDING);
        List<MatchParticipantData> participants = Arrays.asList(p1);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.confirmSchedule("user1", false, null, RebookReasonEnum.TIME_NOT_SUITABLE, 1, 1, TournamentEntryStageEnum.QUALIFY, 0);

        Assert.assertEquals(TournamentMatchStatusEnum.BOOKING, match.getData().getStatus());
        Assert.assertEquals("user1", match.getData().getLastRebookBy());
        Assert.assertEquals(RebookReasonEnum.TIME_NOT_SUITABLE.getCode(), match.getData().getLastRebookReasonCode());
        Assert.assertNotNull(match.getData().getLastRebookTime());
    }

    @Test
    public void testSubmitResult() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.PENDING_PLAY);
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        p1.setEntryNo(1);
        MatchParticipantData p2 = new MatchParticipantData();
        p2.setUserId("user2");
        p2.setEntryNo(2);
        List<MatchParticipantData> participants = Arrays.asList(p1, p2);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.submitResult("user1", 1);

        Assert.assertEquals(TournamentMatchStatusEnum.PENDING_CONFIRM, match.getData().getStatus());
        Assert.assertEquals(Integer.valueOf(1), match.getData().getWinnerEntryNo());
        Assert.assertNotNull(match.getData().getSubmittedTime());
    }

    @Test
    public void testConfirmResult_AllConfirm() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.PENDING_CONFIRM);
        data.setWinnerEntryNo(1);
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        p1.setResultConfirmStatus(ConfirmStatusEnum.CONFIRMED);
        MatchParticipantData p2 = new MatchParticipantData();
        p2.setUserId("user2");
        p2.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
        List<MatchParticipantData> participants = Arrays.asList(p1, p2);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.confirmResult("user2", true, null, 1, 1, TournamentEntryStageEnum.QUALIFY, 0);

        Assert.assertEquals(TournamentMatchStatusEnum.COMPLETED, match.getData().getStatus());
        Assert.assertEquals(ConfirmStatusEnum.CONFIRMED, p2.getResultConfirmStatus());
    }

    @Test
    public void testConfirmResult_AnyReject() {
        TournamentMatchData data = new TournamentMatchData();
        data.setStatus(TournamentMatchStatusEnum.PENDING_CONFIRM);
        LocalDateTime submittedTime = LocalDateTime.now();
        data.setSubmittedTime(submittedTime);
        MatchParticipantData p1 = new MatchParticipantData();
        p1.setUserId("user1");
        p1.setResultConfirmStatus(ConfirmStatusEnum.PENDING);
        List<MatchParticipantData> participants = Arrays.asList(p1);

        TournamentMatch match = new TournamentMatch(data, participants);
        match.confirmResult("user1", false, ResultRejectReasonEnum.DISPUTE_APPEAL, 1, 1, TournamentEntryStageEnum.QUALIFY, 0);

        // 拒绝结果即拒赛：比赛终止（REJECTED），已提交的比分/胜负等记录保留供追溯
        Assert.assertEquals(TournamentMatchStatusEnum.REJECTED, match.getData().getStatus());
        Assert.assertEquals(submittedTime, match.getData().getSubmittedTime());
        Assert.assertEquals(ConfirmStatusEnum.REJECTED, p1.getResultConfirmStatus());
    }
}
