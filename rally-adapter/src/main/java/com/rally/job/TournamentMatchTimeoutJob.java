package com.rally.job;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.service.TournamentMatchFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "job.tournamentMatchTimeout.enabled", havingValue = "true")
public class TournamentMatchTimeoutJob {

    private final TournamentMatchRepository matchRepository;
    private final TournamentMatchFlowService matchFlowService;

    @Scheduled(cron = "${job.tournamentMatchTimeout.cron:0 0 */2 * * ?}")
    public void processTimeoutMatches() {
        log.info("开始处理超时比赛");
        processMatchedTimeout();
        processBookingTimeout();
        processScheduledTimeout();
        processPendingConfirmTimeout();
        log.info("超时比赛处理完成");
    }

    private void processMatchedTimeout() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusHours(24);
        List<TournamentMatch> matches = matchRepository.findTimeoutMatches(TournamentMatchStatusEnum.MATCHED, timeoutBefore);
        log.info("MATCHED状态超时比赛数量: {}", matches.size());
        for (TournamentMatch match : matches) {
            try {
                match.getData().setStatus(TournamentMatchStatusEnum.REJECTED);
                match.getData().setRejectReason("TIMEOUT:无响应超时");
                matchRepository.save(match.getData());
            } catch (Exception e) {
                log.error("处理MATCHED超时比赛失败, matchId={}", match.getMatchId(), e);
            }
        }
    }

    private void processBookingTimeout() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusHours(48);
        List<TournamentMatch> matches = matchRepository.findTimeoutMatches(TournamentMatchStatusEnum.BOOKING, timeoutBefore);
        log.info("BOOKING状态超时比赛数量: {}", matches.size());
        for (TournamentMatch match : matches) {
            try {
                match.getData().setStatus(TournamentMatchStatusEnum.REJECTED);
                match.getData().setRejectReason("TIMEOUT:订场人无响应超时");
                matchRepository.save(match.getData());
            } catch (Exception e) {
                log.error("处理BOOKING超时比赛失败, matchId={}", match.getMatchId(), e);
            }
        }
    }

    private void processScheduledTimeout() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusHours(48);
        List<TournamentMatch> matches = matchRepository.findTimeoutMatches(TournamentMatchStatusEnum.SCHEDULED, timeoutBefore);
        log.info("SCHEDULED状态超时比赛数量: {}", matches.size());
        for (TournamentMatch match : matches) {
            try {
                match.getParticipants().forEach(p -> {
                    if (p.getConfirmStatus() == com.rally.domain.tournament.enums.ConfirmStatusEnum.PENDING) {
                        p.setConfirmStatus(com.rally.domain.tournament.enums.ConfirmStatusEnum.CONFIRMED);
                        p.setConfirmTime(LocalDateTime.now());
                    }
                });
                match.getData().setStatus(TournamentMatchStatusEnum.PENDING_PLAY);
                matchRepository.save(match.getData());
                matchRepository.saveParticipants(match.getParticipants());
            } catch (Exception e) {
                log.error("处理SCHEDULED超时比赛失败, matchId={}", match.getMatchId(), e);
            }
        }
    }

    private void processPendingConfirmTimeout() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusHours(48);
        List<TournamentMatch> matches = matchRepository.findTimeoutMatches(TournamentMatchStatusEnum.PENDING_CONFIRM, timeoutBefore);
        log.info("PENDING_CONFIRM状态超时比赛数量: {}", matches.size());
        for (TournamentMatch match : matches) {
            try {
                match.getParticipants().forEach(p -> {
                    if (p.getResultConfirmStatus() == com.rally.domain.tournament.enums.ConfirmStatusEnum.PENDING) {
                        p.setResultConfirmStatus(com.rally.domain.tournament.enums.ConfirmStatusEnum.CONFIRMED);
                        p.setResultConfirmTime(LocalDateTime.now());
                    }
                });
                match.getData().setStatus(TournamentMatchStatusEnum.COMPLETED);
                matchRepository.save(match.getData());
                matchRepository.saveParticipants(match.getParticipants());
            } catch (Exception e) {
                log.error("处理PENDING_CONFIRM超时比赛失败, matchId={}", match.getMatchId(), e);
            }
        }
    }
}
