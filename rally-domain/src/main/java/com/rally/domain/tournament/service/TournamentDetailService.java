package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.convert.TournamentDomainConvertMapper;
import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import com.rally.domain.tournament.enums.TournamentDisplayStatusEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchOpponentDTO;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.MatchParticipantDTO;
import com.rally.domain.tournament.model.MyCurrentMatchDTO;
import com.rally.domain.tournament.model.TournamentActionDTO;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentBracketDTO;
import com.rally.domain.tournament.model.TournamentBracketMatchDTO;
import com.rally.domain.tournament.model.TournamentBracketRoundDTO;
import com.rally.domain.tournament.model.TournamentRejectRecordDTO;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.domain.tournament.model.TournamentDTO;
import com.rally.domain.tournament.model.TournamentEntrantDTO;
import com.rally.domain.tournament.model.TournamentEntrantRoundDTO;
import com.rally.domain.tournament.model.TournamentEntrantsDTO;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.tournament.model.TournamentOfflineDTO;
import com.rally.domain.tournament.model.TournamentProgressDTO;
import com.rally.domain.tournament.model.TournamentTimelineEventDTO;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 落地页详情聚合领域服务：装配赛事/进程/我的报名/我的比赛/actionState/时间线/签表/信用记录，
 * 并记录已报名用户最近一次访问详情的时间
 * 不依赖用户域，昵称等展示信息由 app 层批量查询后回填
 */
@Service
@RequiredArgsConstructor
public class TournamentDetailService {

    private final TournamentRepository tournamentRepository;

    private final TournamentEntryRepository tournamentEntryRepository;

    private final TournamentMatchRepository tournamentMatchRepository;

    private final TournamentEntryService tournamentEntryService;

    /**
     * 聚合装配赛事详情，userId 为空时只返回公开区块，并标记为未登录状态
     */
    public TournamentDetailDTO assembleDetail(String tournamentId, String userId) {
        TournamentData tournamentData = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournamentData, BizErrorCode.TOURNAMENT_NOT_FOUND);

        List<TournamentMatchData> allMatches = tournamentMatchRepository.findByTournamentId(tournamentId);
        List<TournamentEntryData> allEntries = tournamentEntryRepository.findByTournamentId(tournamentId);

        TournamentDetailDTO detail = new TournamentDetailDTO();
        TournamentDTO tournamentDTO = TournamentDomainConvertMapper.INSTANCE.toTournamentDTO(tournamentData);
        calculateStatus(tournamentDTO, tournamentData);
        detail.setTournament(tournamentDTO);
        if (tournamentData.getOfflineMeetupId() != null) {
            TournamentOfflineDTO offline = new TournamentOfflineDTO();
            offline.setMeetupId(tournamentData.getOfflineMeetupId());
            detail.setOffline(offline);
        }
        detail.setProgress(assembleProgress(tournamentData, allMatches, allEntries.size()));
        detail.setBracket(assembleBracket(tournamentData, allMatches));
        detail.setRejectRecords(assembleRejectRecords(allEntries));
        detail.setEntrants(assembleEntrants(allEntries));
        detail.setEntrantOverview(assembleEntrantOverview(tournamentData, allEntries));

        if (userId == null) {
            setAction(detail, TournamentActionStateEnum.NOT_LOGGED_IN);
            return detail;
        }

        TournamentEntryData myEntryData = tournamentEntryRepository.findByTournamentAndUser(tournamentId, userId);
        if (myEntryData == null) {
            setAction(detail, calculateNotRegisteredActionState(tournamentData));
            return detail;
        }

        LocalDateTime lastVisitTime = LocalDateTime.now();
        tournamentEntryRepository.updateLastVisitTime(tournamentId, userId, lastVisitTime);
        myEntryData.setLastVisitTime(lastVisitTime);

        TournamentEntryDTO myEntry = TournamentDomainConvertMapper.INSTANCE.toTournamentEntryDTO(myEntryData);
        detail.setMyEntry(myEntry);

        TournamentMatch activeMatch = null;
        if (myEntryData.getStatus() == TournamentEntryStatusEnum.IN_MATCH) {
            activeMatch = tournamentMatchRepository.findActiveMatchByTournamentAndUser(tournamentId, userId);
            detail.setMyCurrentMatch(toMyCurrentMatchDTO(activeMatch, myEntryData.getEntryNo(), tournamentData, allEntries));
        }

        setAction(detail, calculateActionState(tournamentData, myEntryData, activeMatch, userId));
        detail.setMyTimeline(assembleTimeline(tournamentId, userId, myEntryData));
        return detail;
    }

    private void calculateStatus(TournamentDTO tournamentDTO, TournamentData tournamentData) {
        tournamentDTO.setMatchTypeShow(tournamentData.getMatchType().getName());
        tournamentDTO.setGenderLimitShow(tournamentData.getGenderLimit().getLabel());
        tournamentDTO.setOfflineFromRoundShow(tournamentData.getOfflineFromRound() == null ? null : tournamentData.getOfflineFromRound().getLabel());
        if (tournamentData.getStatus() == TournamentStatusEnum.ABANDONED) {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.ABANDONED);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.ABANDONED.getLabel());
            return;
        }
        if (tournamentData.getStatus() == TournamentStatusEnum.FINISHED) {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.ENDED);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.ENDED.getLabel());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime registrationStartTime = tournamentData.getRegistrationStartTime();
        LocalDateTime qualifierStartTime = tournamentData.getQualifierStartTime();
        LocalDateTime endTime = tournamentData.getEndTime();

        if (endTime != null && now.isAfter(endTime)) {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.ENDED);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.ENDED.getLabel());
        } else if (now.isBefore(registrationStartTime)) {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.NOT_STARTED);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.NOT_STARTED.getLabel());
        } else if (qualifierStartTime == null || now.isBefore(qualifierStartTime)) {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.REGISTRATION);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.REGISTRATION.getLabel());
        } else {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.IN_PROGRESS);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.IN_PROGRESS.getLabel());
        }
    }

    private TournamentProgressDTO assembleProgress(TournamentData tournamentData, List<TournamentMatchData> allMatches, int entryCount) {
        TournamentProgressDTO progress = new TournamentProgressDTO();
        progress.setEntryCount(entryCount);
        progress.setTotalSlots(tournamentData.getTotalSlots());
        progress.setTotalMatchCount(allMatches.size());
        progress.setRegistrationEndTime(tournamentData.getRegistrationEndTime());
        progress.setQualifierEndTime(tournamentData.getQualifierEndTime());

        TournamentRoundEnum currentRound = tournamentData.getCurrentRound();
        progress.setCurrentRound(currentRound);
        progress.setCurrentRoundShow(currentRound == null ? null : currentRound.getLabel());

        if (currentRound != null) {
            List<TournamentMatchData> currentRoundMatches = allMatches.stream().filter(m -> m.getRound() == currentRound).collect(Collectors.toList());
            progress.setCurrentRoundTotalMatches(currentRoundMatches.size());
            progress.setCurrentRoundCompletedMatches((int) currentRoundMatches.stream().filter(m -> m.getStatus() == TournamentMatchStatusEnum.COMPLETED).count());
            progress.setCurrentRoundAdvanceableSlots(currentRound == TournamentRoundEnum.QUALIFIER ? tournamentData.getTotalSlots() : currentRound.getSlotCount());
            progress.setCurrentRoundAdvancedCount(tournamentEntryService.countRoundEntry(tournamentData.getBizId(), currentRound));
        } else {
            progress.setCurrentRoundTotalMatches(0);
            progress.setCurrentRoundCompletedMatches(0);
        }
        progress.setProgressRate(calculateProgressRate(tournamentData, allMatches));
        return progress;
    }

    /** 资格赛与正赛各占 50%：资格赛按晋级正赛人数计算，正赛按已完成场次计算。 */
    private BigDecimal calculateProgressRate(TournamentData tournamentData, List<TournamentMatchData> allMatches) {
        int totalSlots = tournamentData.getTotalSlots();
        int mainDrawTotalMatches = totalSlots - 1;
        if (totalSlots <= 0 || mainDrawTotalMatches <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal qualifierProgress = BigDecimal.valueOf(tournamentEntryService.countRoundEntry(tournamentData.getBizId(), TournamentRoundEnum.QUALIFIER))
                .divide(BigDecimal.valueOf(totalSlots), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
        long completedMainDrawMatches = allMatches.stream()
                .filter(m -> m.getRound() != TournamentRoundEnum.QUALIFIER)
                .filter(m -> m.getStatus() == TournamentMatchStatusEnum.COMPLETED)
                .count();
        BigDecimal mainDrawProgress = BigDecimal.valueOf(completedMainDrawMatches)
                .divide(BigDecimal.valueOf(mainDrawTotalMatches), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        return qualifierProgress.add(mainDrawProgress)
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }


    private TournamentBracketDTO assembleBracket(TournamentData tournamentData, List<TournamentMatchData> allMatches) {
        if (allMatches.isEmpty()) {
            return null;
        }

        Map<TournamentRoundEnum, List<TournamentMatchData>> byRound = allMatches.stream().collect(Collectors.groupingBy(TournamentMatchData::getRound));

        List<String> matchIds = allMatches.stream().map(TournamentMatchData::getBizId).collect(Collectors.toList());
        Map<String, List<MatchParticipantData>> participantsByMatch = tournamentMatchRepository.findParticipantsByMatchIds(matchIds).stream()
                .collect(Collectors.groupingBy(MatchParticipantData::getMatchId));

        List<TournamentBracketRoundDTO> rounds = allRounds(tournamentData.getTotalSlots()).stream()
                .map(round -> {
                    TournamentBracketRoundDTO roundDTO = new TournamentBracketRoundDTO();
                    roundDTO.setRound(round);
                    roundDTO.setRoundShow(round.getLabel());
                    List<TournamentBracketMatchDTO> matches = byRound.getOrDefault(round, List.of()).stream()
                            .sorted(Comparator.comparingInt((TournamentMatchData match) -> matchStatusOrder(match.getStatus()))
                                    .thenComparing(TournamentMatchData::getMatchNo, Comparator.nullsLast(Integer::compareTo)))
                            .map(m -> toBracketMatchDTO(m, participantsByMatch.getOrDefault(m.getBizId(), List.of())))
                            .collect(Collectors.toList());
                    roundDTO.setMatches(matches);
                    return roundDTO;
                }).collect(Collectors.toList());

        TournamentBracketDTO bracket = new TournamentBracketDTO();
        bracket.setRounds(rounds);
        return bracket;
    }

    /** 赛事完整轮次序列：资格赛 + 正赛首轮（由 totalSlots 决定）到决赛 */
    private List<TournamentRoundEnum> allRounds(int totalSlots) {
        List<TournamentRoundEnum> rounds = new ArrayList<>();
        rounds.add(TournamentRoundEnum.QUALIFIER);
        TournamentRoundEnum firstMainRound = TournamentRoundEnum.firstMainRound(totalSlots);
        boolean collecting = false;
        for (TournamentRoundEnum round : TournamentRoundEnum.values()) {
            if (round == firstMainRound) {
                collecting = true;
            }
            if (collecting && round != TournamentRoundEnum.QUALIFIER) {
                rounds.add(round);
            }
        }
        return rounds;
    }

    private TournamentBracketMatchDTO toBracketMatchDTO(TournamentMatchData matchData, List<MatchParticipantData> participants) {
        TournamentBracketMatchDTO dto = new TournamentBracketMatchDTO();
        dto.setMatchId(matchData.getBizId());
        dto.setMatchNo(matchData.getMatchNo());
        dto.setWinnerEntryNo(matchData.getWinnerEntryNo());
        dto.setStatus(matchData.getStatus());
        dto.setParticipants(participants.stream().map(this::toOpponentDTO).collect(Collectors.toList()));
        return dto;
    }

    private MatchOpponentDTO toOpponentDTO(MatchParticipantData participant) {
        MatchOpponentDTO dto = new MatchOpponentDTO();
        dto.setUserId(participant.getUserId());
        dto.setEntryNo(participant.getEntryNo());
        return dto;
    }

    /** 已结束、进行中及其他状态、已终止。 */
    private int matchStatusOrder(TournamentMatchStatusEnum status) {
        if (status == TournamentMatchStatusEnum.COMPLETED) {
            return 0;
        }
        if (status == TournamentMatchStatusEnum.REJECTED) {
            return 2;
        }
        return 1;
    }

    private MyCurrentMatchDTO toMyCurrentMatchDTO(TournamentMatch match, Integer currentEntryNo,
                                                   TournamentData tournamentData, List<TournamentEntryData> allEntries) {
        if (match == null) {
            return null;
        }
        TournamentMatchData data = match.getData();
        MyCurrentMatchDTO dto = new MyCurrentMatchDTO();
        dto.setMatchId(data.getBizId());
        dto.setRound(data.getRound());
        dto.setCourtBookerId(data.getCourtBookerId());
        dto.setMeetupId(data.getMeetupId());
        dto.setWinnerEntryNo(data.getWinnerEntryNo());
        dto.setLastRebookBy(data.getLastRebookBy());
        dto.setLastRebookReasonCode(data.getLastRebookReasonCode());
        dto.setLastRebookTime(data.getLastRebookTime());
        dto.setStatus(data.getStatus());
        dto.setGroupSize(data.getRound() == TournamentRoundEnum.QUALIFIER ? tournamentData.getQualifierGroupSize() : 2);

        List<MatchParticipantData> opponentParticipants = match.getParticipants().stream()
                .filter(p -> currentEntryNo != null && p.getEntryNo() != null && !Objects.equals(p.getEntryNo(), currentEntryNo))
                .collect(Collectors.toList());

        Map<String, TournamentEntryData> entriesByUser = allEntries.stream()
                .collect(Collectors.toMap(TournamentEntryData::getUserId, entry -> entry, (first, ignored) -> first));

        if (data.getStatus() == TournamentMatchStatusEnum.BOOKING) {
            dto.setOpponentEntries(opponentParticipants.stream()
                    .map(p -> entriesByUser.get(p.getUserId()))
                    .filter(java.util.Objects::nonNull)
                    .map(TournamentDomainConvertMapper.INSTANCE::toTournamentEntryDTO)
                    .collect(Collectors.toList()));
        }

        dto.setParticipants(match.getParticipants().stream().map(p -> {
            MatchParticipantDTO participantDTO = new MatchParticipantDTO();
            participantDTO.setUserId(p.getUserId());
            participantDTO.setEntryNo(p.getEntryNo());
            if (currentEntryNo != null && p.getEntryNo() != null && !Objects.equals(p.getEntryNo(), currentEntryNo)) {
                TournamentEntryData opponentEntry = entriesByUser.get(p.getUserId());
                participantDTO.setLastVisitTime(opponentEntry == null ? null : opponentEntry.getLastVisitTime());
            }
            participantDTO.setConfirmStatus(p.getConfirmStatus());
            participantDTO.setResultConfirmStatus(p.getResultConfirmStatus());
            return participantDTO;
        }).collect(Collectors.toList()));

        return dto;
    }

    TournamentActionStateEnum calculateActionState(TournamentData tournamentData, TournamentEntryData entry, TournamentMatch activeMatch, String userId) {
        TournamentEntryStatusEnum status = entry.getStatus();
        if (status == TournamentEntryStatusEnum.CHAMPION) {
            return TournamentActionStateEnum.CHAMPION;
        }
        if (tournamentData.getStatus() == TournamentStatusEnum.FINISHED
                || tournamentData.getEndTime() != null && LocalDateTime.now().isAfter(tournamentData.getEndTime())) {
            return TournamentActionStateEnum.END;
        }
        if (status == TournamentEntryStatusEnum.WITHDRAWN) {
            return TournamentActionStateEnum.WITHDRAWN;
        }
        if (status == TournamentEntryStatusEnum.ELIMINATED) {
            return TournamentActionStateEnum.ELIMINATED;
        }
        if (status == TournamentEntryStatusEnum.FROZEN) {
            return TournamentActionStateEnum.FROZEN;
        }
        if (entry.getCurrentRound() == tournamentData.getOfflineFromRound()) {
            return TournamentActionStateEnum.IN_OFFLINE_STAGE;
        }
        if (status == TournamentEntryStatusEnum.PAYING) {
            return TournamentActionStateEnum.AWAIT_PAYMENT;
        }
        if (status == TournamentEntryStatusEnum.WAITING) {
            if (isAdvancedToLaterRound(tournamentData, entry)) {
                return TournamentActionStateEnum.ADVANCED;
            }
            if (tournamentData.getQualifierStartTime() == null || LocalDateTime.now().isBefore(tournamentData.getQualifierStartTime())) {
                return TournamentActionStateEnum.AWAIT_QUALIFIER_START;
            }
            return TournamentActionStateEnum.WAITING_MATCH;
        }
        Assert.isTrue(status == TournamentEntryStatusEnum.IN_MATCH, BizErrorCode.TOURNAMENT_ENTRY_STATUS_ILLEGAL);
        if (activeMatch == null) {
            return TournamentActionStateEnum.WAITING_MATCH;
        }
        TournamentMatchData matchData = activeMatch.getData();
        switch (matchData.getStatus()) {
            case MATCHED:
                return TournamentActionStateEnum.AWAIT_COURT_BOOKER_SELECT;
            case BOOKING:
                if (userId.equals(matchData.getCourtBookerId())) {
                    return matchData.getLastRebookTime() == null
                            ? TournamentActionStateEnum.AWAIT_BOOKING
                            : TournamentActionStateEnum.AWAIT_BOOKING_REBOOK;
                }
                return TournamentActionStateEnum.AWAIT_BOOKING_OPPONENT;
            case SCHEDULED:
                if (isPending(activeMatch, userId, false)) {
                    return TournamentActionStateEnum.AWAIT_SCHEDULE_CONFIRM;
                }
                return userId.equals(matchData.getCourtBookerId()) ? TournamentActionStateEnum.AWAIT_OPPONENT_SCHEDULE_CONFIRM : TournamentActionStateEnum.WAITING_MATCH;
            case PENDING_PLAY:
                return TournamentActionStateEnum.AWAIT_RESULT_SUBMIT;
            case PENDING_CONFIRM:
                if (isPending(activeMatch, userId, true)) {
                    return TournamentActionStateEnum.AWAIT_RESULT_CONFIRM;
                }
                return userId.equals(matchData.getSubmitterUserId()) ? TournamentActionStateEnum.AWAIT_OPPONENT_RESULT_CONFIRM : TournamentActionStateEnum.WAITING_MATCH;
            default:
                return TournamentActionStateEnum.WAITING_MATCH;
        }
    }

    private boolean isAdvancedToLaterRound(TournamentData tournamentData, TournamentEntryData entry) {
        TournamentRoundEnum tournamentRound = tournamentData.getCurrentRound();
        TournamentRoundEnum entryRound = entry.getCurrentRound();
        return tournamentRound != null && entryRound != null && entryRound.ordinal() > tournamentRound.ordinal();
    }

    private TournamentActionStateEnum calculateNotRegisteredActionState(TournamentData tournamentData) {
        boolean registrationClosed = tournamentData.getRegistrationEndTime() != null
                && LocalDateTime.now().isAfter(tournamentData.getRegistrationEndTime());
        boolean enteredNonQualifierStage = tournamentData.getCurrentRound() != null
                && tournamentData.getCurrentRound() != TournamentRoundEnum.QUALIFIER;
        return registrationClosed || enteredNonQualifierStage
                ? TournamentActionStateEnum.NOT_REGISTERED_CLOSED
                : TournamentActionStateEnum.NOT_REGISTERED;
    }

    private void setAction(TournamentDetailDTO detail, TournamentActionStateEnum state) {
        TournamentActionDTO action = new TournamentActionDTO();
        action.setState(state);
        detail.setAction(action);
    }

    private boolean isPending(TournamentMatch match, String userId, boolean resultConfirm) {
        return match.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .map(p -> (resultConfirm ? p.getResultConfirmStatus() : p.getConfirmStatus()) == ConfirmStatusEnum.PENDING)
                .orElse(false);
    }

    private List<TournamentTimelineEventDTO> assembleTimeline(String tournamentId, String userId, TournamentEntryData entry) {
        List<TournamentTimelineEventDTO> events = new ArrayList<>();
        events.add(new TournamentTimelineEventDTO(entry.getCreateTime(), "报名成功"));
        if (entry.getQualifiedTime() != null) {
            events.add(new TournamentTimelineEventDTO(entry.getQualifiedTime(), "获得正赛资格"));
        }
        if (entry.getPaidTime() != null) {
            events.add(new TournamentTimelineEventDTO(entry.getPaidTime(), "支付成功，锁定正赛席位"));
        }

        List<MatchParticipantData> myParticipations = tournamentMatchRepository.findParticipantsByMatchIds(
                tournamentMatchRepository.findByTournamentId(tournamentId).stream().map(TournamentMatchData::getBizId).collect(Collectors.toList())
        ).stream().filter(p -> p.getUserId().equals(userId)).collect(Collectors.toList());

        Set<String> myMatchIds = myParticipations.stream().map(MatchParticipantData::getMatchId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<TournamentMatchData> myMatches = tournamentMatchRepository.findByTournamentId(tournamentId).stream()
                .filter(m -> myMatchIds.contains(m.getBizId()))
                .collect(Collectors.toList());

        for (TournamentMatchData match : myMatches) {
            if (match.getMatchedTime() != null) {
                events.add(new TournamentTimelineEventDTO(match.getMatchedTime(), "匹配成功"));
            }
            if (match.getCourtBookerSelectedTime() != null) {
                events.add(new TournamentTimelineEventDTO(match.getCourtBookerSelectedTime(), "确定订场人"));
            }
            if (match.getScheduleSubmittedTime() != null) {
                events.add(new TournamentTimelineEventDTO(match.getScheduleSubmittedTime(), "提交赛约"));
            }
            if (match.getSubmittedTime() != null) {
                events.add(new TournamentTimelineEventDTO(match.getSubmittedTime(), "提交比赛结果"));
            }
            if (match.getCompletedTime() != null) {
                events.add(new TournamentTimelineEventDTO(match.getCompletedTime(), "比赛完成"));
            }
        }

        events.sort(Comparator.comparing(TournamentTimelineEventDTO::getTime));
        return events;
    }

    private List<TournamentRejectRecordDTO> assembleRejectRecords(List<TournamentEntryData> entries) {
        List<TournamentRejectRecordDTO> records = new ArrayList<>();
        for (TournamentEntryData entry : entries) {
            int rejectCount = entry.getQualifierRejectCount() + entry.getMainDrawRejectCount();
            if (rejectCount > 0) {
                records.add(rejectRecord(entry.getUserId(), rejectCount));
            }
        }
        return records;
    }

    private TournamentRejectRecordDTO rejectRecord(String userId, int rejectCount) {
        TournamentRejectRecordDTO record = new TournamentRejectRecordDTO();
        record.setUserId(userId);
        record.setRejectCount(rejectCount);
        return record;
    }

    private List<TournamentEntrantDTO> assembleEntrants(List<TournamentEntryData> entries) {
        return entries.stream()
                .sorted(Comparator.comparing(TournamentEntryData::getEntryNo, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toEntrantDTO)
                .collect(Collectors.toList());
    }

    private TournamentEntrantsDTO assembleEntrantOverview(TournamentData tournamentData, List<TournamentEntryData> entries) {
        Map<TournamentRoundEnum, List<TournamentEntryData>> entriesByRound = entries.stream()
                .filter(entry -> entry.getStatus() != TournamentEntryStatusEnum.WITHDRAWN)
                .filter(entry -> entry.getCurrentRound() != null)
                .collect(Collectors.groupingBy(TournamentEntryData::getCurrentRound));

        Comparator<TournamentEntryData> entrantOrder = Comparator
                .comparingInt((TournamentEntryData entry) -> entrantStatusOrder(entry.getStatus()))
                .thenComparing(TournamentEntryData::getEntryNo, Comparator.nullsLast(Integer::compareTo));

        List<TournamentEntrantRoundDTO> rounds = allRounds(tournamentData.getTotalSlots()).stream()
                .map(round -> {
                    TournamentEntrantRoundDTO roundDTO = new TournamentEntrantRoundDTO();
                    roundDTO.setRound(round);
                    roundDTO.setRoundShow(round.getLabel());
                    roundDTO.setEntrants(entriesByRound.getOrDefault(round, List.of()).stream()
                            .sorted(entrantOrder)
                            .map(this::toEntrantDTO)
                            .collect(Collectors.toList()));
                    return roundDTO;
                })
                .collect(Collectors.toList());

        TournamentEntrantsDTO entrants = new TournamentEntrantsDTO();
        entrants.setTotalCount(entries.size());
        entrants.setWithdrawnCount((int) entries.stream()
                .filter(entry -> entry.getStatus() == TournamentEntryStatusEnum.WITHDRAWN)
                .count());
        entrants.setRounds(rounds);
        return entrants;
    }

    private TournamentEntrantDTO toEntrantDTO(TournamentEntryData entry) {
        TournamentEntrantDTO dto = new TournamentEntrantDTO();
        dto.setUserId(entry.getUserId());
        dto.setEntryNo(entry.getEntryNo());
        dto.setEntryNoShow(entry.getEntryNo() == null ? null : String.format("%03d", entry.getEntryNo()));
        dto.setStatus(entry.getStatus());
        dto.setStatusShow(entry.getStatus() == null ? null : entry.getStatus().getLabel());
        return dto;
    }

    /** 比赛中、等待匹配、其他状态。 */
    private int entrantStatusOrder(TournamentEntryStatusEnum status) {
        if (status == TournamentEntryStatusEnum.IN_MATCH) {
            return 0;
        }
        if (status == TournamentEntryStatusEnum.WAITING) {
            return 1;
        }
        return 2;
    }
}
