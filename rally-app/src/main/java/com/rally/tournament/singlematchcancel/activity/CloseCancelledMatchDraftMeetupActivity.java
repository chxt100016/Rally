package com.rally.tournament.singlematchcancel.activity;

import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.gateway.RegistrationRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.tournament.match.TournamentMatchCancellationSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 close-cancelled-match-draft-meetup：关闭被取消比赛仍为草稿的关联赛约。
 */
@Component
@RequiredArgsConstructor
public class CloseCancelledMatchDraftMeetupActivity {

    private final MeetupRepository meetupRepository;
    private final RegistrationRepository registrationRepository;
    private final MeetupDomainService meetupDomainService;

    /**
     * 无关联赛约、赛约缺失或持久化状态已非草稿时幂等跳过，并始终透传取消快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public TournamentMatchCancellationSnapshot execute(
            TournamentMatchCancellationSnapshot cancellationSnapshot) {
        // A1：取消快照中的赛约编号为空时无需联动。
        String meetupId = cancellationSnapshot.meetupId();
        if (meetupId == null || meetupId.isBlank()) {
            return cancellationSnapshot;
        }

        // A2：只依据持久化状态处理 DRAFT；缺失及其他状态均按幂等无需变更处理。
        MeetupData meetupData = meetupRepository.findByBizId(meetupId);
        if (meetupData == null || meetupData.getStatus() != MeetupStatusEnum.DRAFT) {
            return cancellationSnapshot;
        }

        Meetup meetup = new Meetup(
                meetupData,
                registrationRepository.findByMeetupId(meetupId));
        meetupDomainService.closeTournamentDraft(meetup, true);

        // A3：后续报名释放活动继续使用同一个不可变取消快照。
        return cancellationSnapshot;
    }
}
