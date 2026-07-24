package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.convert.TournamentDomainConvertMapper;
import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import com.rally.domain.tournament.enums.TournamentDisplayStatusEnum;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
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
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentBracketDTO;
import com.rally.domain.tournament.model.TournamentBracketMatchDTO;
import com.rally.domain.tournament.model.TournamentBracketRoundDTO;
import com.rally.domain.tournament.model.TournamentRejectRecordDTO;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.domain.tournament.model.TournamentDTO;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.tournament.model.TournamentProgressDTO;
import com.rally.domain.tournament.model.TournamentTimelineEventDTO;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 落地页详情聚合领域服务：只读装配赛事/进程/我的报名/我的比赛/actionState/时间线/签表/信用记录
 * 不依赖用户域，昵称等展示信息由 app 层批量查询后回填
 */
@Service
@RequiredArgsConstructor
public class TournamentDetailService {

    private final TournamentRepository tournamentRepository;

    private final TournamentEntryRepository tournamentEntryRepository;

    private final TournamentMatchRepository tournamentMatchRepository;

    /**
     * 聚合装配赛事详情，userId 为空时只返回公开区块（tournament + progress + bracket）
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
        detail.setProgress(assembleProgress(tournamentData, allMatches, allEntries.size()));
        detail.setBracket(assembleBracket(allMatches));
        detail.setRejectRecords(assembleRejectRecords(allEntries));

        if (userId == null) {
            detail.setActionState(TournamentActionStateEnum.NOT_REGISTERED);
            return detail;
        }

        TournamentEntryData myEntryData = tournamentEntryRepository.findByTournamentAndUser(tournamentId, userId);
        if (myEntryData == null) {
            detail.setActionState(TournamentActionStateEnum.NOT_REGISTERED);
            return detail;
        }

        TournamentEntryDTO myEntry = TournamentDomainConvertMapper.INSTANCE.toTournamentEntryDTO(myEntryData);
        detail.setMyEntry(myEntry);

        TournamentMatch activeMatch = null;
        if (myEntryData.getStatus() == TournamentEntryStatusEnum.IN_MATCH) {
            activeMatch = tournamentMatchRepository.findActiveMatchByTournamentAndUser(tournamentId, userId);
            detail.setMyCurrentMatch(toMyCurrentMatchDTO(activeMatch, userId, tournamentId, tournamentData));
        }

        detail.setActionState(calculateActionState(myEntryData, activeMatch, userId));
        detail.setMyTimeline(assembleTimeline(tournamentId, userId, myEntryData));
        return detail;
    }

    private void calculateStatus(TournamentDTO tournamentDTO, TournamentData tournamentData) {
        if (tournamentData.getStatus() == TournamentStatusEnum.ABANDONED) {
            tournamentDTO.setDisplayStatus(TournamentDisplayStatusEnum.ABANDONED);
            tournamentDTO.setDisplayStatusShow(TournamentDisplayStatusEnum.ABANDONED.getLabel());
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
        } else if (now.isBefore(qualifierStartTime)) {
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
        progress.setCurrentFilledSlots(tournamentData.getCurrentFilledSlots());
        progress.setTotalSlots(tournamentData.getTotalSlots());
        progress.setTotalMatchCount(allMatches.size());
        progress.setRegistrationEndTime(tournamentData.getRegistrationEndTime());
        progress.setQualifierEndTime(tournamentData.getQualifierEndTime());

        TournamentRoundEnum currentRound = allMatches.stream()
                .map(TournamentMatchData::getRound)
                .max(Comparator.comparingInt(this::roundOrder))
                .orElse(null);
        progress.setCurrentRound(currentRound);

        if (currentRound != null) {
            List<TournamentMatchData> currentRoundMatches = allMatches.stream().filter(m -> m.getRound() == currentRound).collect(Collectors.toList());
            progress.setCurrentRoundTotalMatches(currentRoundMatches.size());
            progress.setCurrentRoundCompletedMatches((int) currentRoundMatches.stream().filter(m -> m.getStatus() == TournamentMatchStatusEnum.COMPLETED).count());
        } else {
            progress.setCurrentRoundTotalMatches(0);
            progress.setCurrentRoundCompletedMatches(0);
        }
        return progress;
    }

    private int roundOrder(TournamentRoundEnum round) {
        return round.ordinal();
    }

    private TournamentBracketDTO assembleBracket(List<TournamentMatchData> allMatches) {
        Map<TournamentRoundEnum, List<TournamentMatchData>> byRound = allMatches.stream().collect(Collectors.groupingBy(TournamentMatchData::getRound));

        List<String> matchIds = allMatches.stream().map(TournamentMatchData::getBizId).collect(Collectors.toList());
        Map<String, List<MatchParticipantData>> participantsByMatch = tournamentMatchRepository.findParticipantsByMatchIds(matchIds).stream()
                .collect(Collectors.groupingBy(MatchParticipantData::getMatchId));

        List<TournamentBracketRoundDTO> rounds = byRound.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> roundOrder(e.getKey())))
                .map(entry -> {
                    TournamentBracketRoundDTO roundDTO = new TournamentBracketRoundDTO();
                    roundDTO.setRound(entry.getKey());
                    List<TournamentBracketMatchDTO> matches = entry.getValue().stream()
                            .sorted(Comparator.comparingInt(TournamentMatchData::getMatchNo))
                            .map(m -> toBracketMatchDTO(m, participantsByMatch.getOrDefault(m.getBizId(), List.of())))
                            .collect(Collectors.toList());
                    roundDTO.setMatches(matches);
                    return roundDTO;
                }).collect(Collectors.toList());

        TournamentBracketDTO bracket = new TournamentBracketDTO();
        bracket.setRounds(rounds);
        return bracket;
    }

    private TournamentBracketMatchDTO toBracketMatchDTO(TournamentMatchData matchData, List<MatchParticipantData> participants) {
        TournamentBracketMatchDTO dto = new TournamentBracketMatchDTO();
        dto.setMatchId(matchData.getBizId());
        dto.setMatchNo(matchData.getMatchNo());
        dto.setWinnerId(matchData.getWinnerId());
        dto.setStatus(matchData.getStatus());
        dto.setParticipants(participants.stream().map(this::toOpponentDTO).collect(Collectors.toList()));
        return dto;
    }

    private MatchOpponentDTO toOpponentDTO(MatchParticipantData participant) {
        MatchOpponentDTO dto = new MatchOpponentDTO();
        dto.setUserId(participant.getUserId());
        return dto;
    }

    private MyCurrentMatchDTO toMyCurrentMatchDTO(TournamentMatch match, String userId, String tournamentId, TournamentData tournamentData) {
        if (match == null) {
            return null;
        }
        TournamentMatchData data = match.getData();
        MyCurrentMatchDTO dto = new MyCurrentMatchDTO();
        dto.setMatchId(data.getBizId());
        dto.setRound(data.getRound());
        dto.setCourtBookerId(data.getCourtBookerId());
        dto.setMeetupId(data.getMeetupId());
        dto.setStatus(data.getStatus());
        dto.setGroupSize(data.getRound() == TournamentRoundEnum.QUALIFIER ? tournamentData.getQualifierGroupSize() : 2);

        List<MatchParticipantData> opponentParticipants = match.getParticipants().stream()
                .filter(p -> !p.getUserId().equals(userId))
                .collect(Collectors.toList());

        dto.setOpponents(opponentParticipants.stream().map(this::toOpponentDTO).collect(Collectors.toList()));

        if (data.getStatus() == TournamentMatchStatusEnum.BOOKING) {
            dto.setOpponentEntries(opponentParticipants.stream()
                    .map(p -> tournamentEntryRepository.findByTournamentAndUser(tournamentId, p.getUserId()))
                    .filter(java.util.Objects::nonNull)
                    .map(TournamentDomainConvertMapper.INSTANCE::toTournamentEntryDTO)
                    .collect(Collectors.toList()));
        }

        dto.setParticipants(match.getParticipants().stream().map(p -> {
            MatchParticipantDTO participantDTO = new MatchParticipantDTO();
            participantDTO.setUserId(p.getUserId());
            participantDTO.setTeamId(p.getTeamId());
            participantDTO.setConfirmStatus(p.getConfirmStatus());
            participantDTO.setResultConfirmStatus(p.getResultConfirmStatus());
            participantDTO.setIsWinner(p.getIsWinner());
            return participantDTO;
        }).collect(Collectors.toList()));

        return dto;
    }

    private TournamentActionStateEnum calculateActionState(TournamentEntryData entry, TournamentMatch activeMatch, String userId) {
        TournamentEntryStatusEnum status = entry.getStatus();
        if (status == TournamentEntryStatusEnum.WITHDRAWN) {
            return TournamentActionStateEnum.WITHDRAWN;
        }
        if (status == TournamentEntryStatusEnum.ELIMINATED) {
            return TournamentActionStateEnum.ELIMINATED;
        }
        if (status == TournamentEntryStatusEnum.PAYING) {
            return TournamentActionStateEnum.AWAIT_PAYMENT;
        }
        if (status == TournamentEntryStatusEnum.WAITING) {
            return entry.getStage() == TournamentEntryStageEnum.MAIN ? TournamentActionStateEnum.QUALIFIED_MAIN_DRAW : TournamentActionStateEnum.WAITING_MATCH;
        }
        // IN_MATCH
        if (activeMatch == null) {
            return TournamentActionStateEnum.WAITING_MATCH;
        }
        TournamentMatchData matchData = activeMatch.getData();
        switch (matchData.getStatus()) {
            case MATCHED:
                return TournamentActionStateEnum.AWAIT_COURT_BOOKER_SELECT;
            case BOOKING:
                return userId.equals(matchData.getCourtBookerId()) ? TournamentActionStateEnum.AWAIT_BOOKING : TournamentActionStateEnum.AWAIT_BOOKING_OPPONENT;
            case SCHEDULED:
                if (isPending(activeMatch, userId, false)) {
                    return TournamentActionStateEnum.AWAIT_SCHEDULE_CONFIRM;
                }
                return userId.equals(matchData.getCourtBookerId()) ? TournamentActionStateEnum.AWAIT_OPPONENT_SCHEDULE_CONFIRM : TournamentActionStateEnum.WAITING_MATCH;
            case PENDING_PLAY:
                return TournamentActionStateEnum.AWAIT_RESULT_SUBMIT;
            case PENDING_CONFIRM:
                return isPending(activeMatch, userId, true) ? TournamentActionStateEnum.AWAIT_RESULT_CONFIRM : TournamentActionStateEnum.WAITING_MATCH;
            default:
                return TournamentActionStateEnum.WAITING_MATCH;
        }
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
}
