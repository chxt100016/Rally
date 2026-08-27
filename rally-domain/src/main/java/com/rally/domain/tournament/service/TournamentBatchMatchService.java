package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.tournament.entry.TournamentEntryStatus;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.tournament.match.TournamentMatchRound;
import com.rally.domain.tournament.matchmaking.CompletedPairing;
import com.rally.domain.tournament.matchmaking.MatchmakingCandidate;
import com.rally.domain.tournament.matchmaking.MatchmakingGroup;
import com.rally.domain.tournament.matchmaking.MatchmakingMember;
import com.rally.domain.tournament.matchmaking.MatchmakingRejection;
import com.rally.domain.tournament.matchmaking.MatchmakingRequest;
import com.rally.domain.tournament.matchmaking.MatchmakingResult;
import com.rally.domain.tournament.matchmaking.TournamentMatchmakingService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.gateway.UserProfileRepository;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 赛事当前轮次的批量匹配能力：自动全局匹配或运营手工指定分组。 */
@Service
@RequiredArgsConstructor
public class TournamentBatchMatchService {
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentMatchmakingService tournamentMatchmakingService;
    private final TournamentMatchAssembleService tournamentMatchAssembleService;
    private final UserProfileRepository userProfileRepository;

    /** 查询已到资格赛开始时间、可由 Job 扫描的激活赛事。 */
    public List<TournamentData> listTournamentsToMatch(LocalDateTime matchTime) {
        return tournamentRepository.findActiveWithQualifierStarted(matchTime);
    }

    /**
     * 自动匹配赛事当前轮次。只读取 tournament.currentRound，不在此处判断或推进赛事轮次。
     */
    @Transactional
    public List<TournamentMatch> matchCurrentRound(String tournamentId) {
        return matchCurrentRound(tournamentId, null);
    }

    /** 自动匹配赛事当前轮次，并可临时排除多个 entryNo；排除仅影响本次计算。 */
    @Transactional
    public List<TournamentMatch> matchCurrentRound(String tournamentId, List<Integer> excludedEntryNos) {
        TournamentData tournament = getTournament(tournamentId);
        return matchCurrentRound(tournament, null, excludedEntryNos);
    }

    /**
     * 按运营指定的 entryNo 分组生成当前轮次比赛。每个 entryNo 必须唯一、属于当前轮次且处于 WAITING；
     * 正赛每组固定两队，资格赛每组必须等于 qualifierGroupSize。手工分组不应用地区、性别和历史对阵限制；
     * 手工分组落地后，会继续自动匹配剩余 WAITING 队伍。
     */
    @Transactional
    public List<TournamentMatch> matchCurrentRoundManually(String tournamentId, List<List<Integer>> manualGroups) {
        return matchCurrentRoundManually(tournamentId, manualGroups, null);
    }

    /** 手工分组并可临时排除多个 entryNo；被排除队伍若写入 manualGroups 会校验失败，剩余队伍自动补齐。 */
    @Transactional
    public List<TournamentMatch> matchCurrentRoundManually(String tournamentId, List<List<Integer>> manualGroups, List<Integer> excludedEntryNos) {
        Assert.isTrue(manualGroups != null && !manualGroups.isEmpty(), BizErrorCode.PARAM_ERROR);
        TournamentData tournament = getTournament(tournamentId);
        return matchCurrentRound(tournament, manualGroups, excludedEntryNos);
    }

    /**
     * 取消尚未提交订场信息的比赛。仅允许 MATCHED、BOOKING；删除比赛与参与者，并把原参赛队恢复为 WAITING。
     */
    @Transactional
    public void cancelUnsubmittedMatch(String matchId) {
        TournamentMatch match = tournamentMatchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        TournamentMatchStatusEnum status = match.getData().getStatus();
        Assert.isTrue(status == TournamentMatchStatusEnum.MATCHED || status == TournamentMatchStatusEnum.BOOKING,
                BizErrorCode.TOURNAMENT_MATCH_CANCEL_FORBIDDEN);
        if (!tournamentMatchRepository.deleteUnsubmittedWithParticipants(matchId)) {
            throw new com.rally.domain.auth.exception.BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        for (MatchParticipantData participant : match.getParticipants()) {
            TournamentEntryData entry = tournamentEntryRepository.findByTournamentAndUser(match.getData().getTournamentId(), participant.getUserId());
            if (entry != null && entry.getStatus() == TournamentEntryStatusEnum.IN_MATCH) {
                entry.setStatus(TournamentEntryStatusEnum.WAITING);
                tournamentEntryRepository.save(entry);
            }
        }
    }

    /**
     * 批量取消一个赛事中所有尚未提交订场信息的比赛（MATCHED、BOOKING）。已进入 SCHEDULED 及后续状态的比赛不受影响。
     */
    @Transactional
    public void cancelUnsubmittedMatches(String tournamentId) {
        getTournament(tournamentId);
        List<String> matchIds = tournamentMatchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> match.getStatus() == TournamentMatchStatusEnum.MATCHED || match.getStatus() == TournamentMatchStatusEnum.BOOKING)
                .map(TournamentMatchData::getBizId)
                .toList();
        for (String matchId : matchIds) {
            cancelUnsubmittedMatch(matchId);
        }
    }

    private TournamentData getTournament(String tournamentId) {
        Assert.notBlank(tournamentId, BizErrorCode.PARAM_ERROR);
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);
        Assert.notNull(tournament.getCurrentRound(), BizErrorCode.PARAM_ERROR);
        return tournament;
    }

    private int groupSize(TournamentData tournament) {
        return tournament.getCurrentRound() == TournamentRoundEnum.QUALIFIER ? tournament.getQualifierGroupSize() : 2;
    }

    private List<TournamentMatch> matchCurrentRound(
            TournamentData tournament,
            List<List<Integer>> manualGroups,
            List<Integer> excludedEntryNos) {
        int groupSize = groupSize(tournament);
        List<CandidateContext> contexts = waitingCandidates(tournament);
        Map<Integer, CandidateContext> contextByEntryNo = contexts.stream()
                .collect(Collectors.toMap(context -> context.candidate().entryNo(), context -> context));
        Set<Integer> excluded = excludedEntryNos == null
                ? Set.of() : new HashSet<>(excludedEntryNos);

        MatchmakingResult result = tournamentMatchmakingService.match(new MatchmakingRequest(
                TournamentMatchRound.valueOf(tournament.getCurrentRound().name()),
                groupSize,
                contexts.stream().map(CandidateContext::candidate).toList(),
                excluded,
                manualGroups,
                completedPairings(tournament.getBizId(), tournament.getCurrentRound())));
        if (!result.isAccepted()) {
            throw matchmakingRejection(result.getRejection(), manualGroups, excluded, contextByEntryNo);
        }

        List<List<TournamentEntryData>> groups = result.getGroups().stream()
                .map(group -> entriesForGroup(group, contextByEntryNo))
                .toList();
        return tournamentMatchAssembleService.assemble(
                tournament.getBizId(), groups, tournament.getCurrentRound(), groupSize);
    }

    private List<CandidateContext> waitingCandidates(TournamentData tournament) {
        List<TournamentEntryData> entries = tournamentEntryRepository.findByTournamentId(tournament.getBizId()).stream()
                .filter(entry -> entry.getStatus() == TournamentEntryStatusEnum.WAITING)
                .filter(entry -> entry.getCurrentRound() == tournament.getCurrentRound())
                .toList();
        Map<Integer, List<TournamentEntryData>> byEntryNo = entries.stream().collect(Collectors.groupingBy(TournamentEntryData::getEntryNo));
        List<CandidateContext> candidates = new ArrayList<>();
        for (Map.Entry<Integer, List<TournamentEntryData>> item : byEntryNo.entrySet()) {
            List<TournamentEntryData> members = item.getValue();
            // 双打队伍尚未由两位成员完成报名时，不能参加匹配。
            if (tournament.getMatchType() == MatchTypeEnum.DOUBLE && members.size() != 2) {
                continue;
            }
            List<UserProfile> profiles = userProfileRepository.findByUserIds(
                    members.stream().map(TournamentEntryData::getUserId).toList());
            List<MatchmakingMember> memberSnapshots = new ArrayList<>();
            for (int index = 0; index < members.size(); index++) {
                TournamentEntryData member = members.get(index);
                UserProfile profile = index < profiles.size() ? profiles.get(index) : null;
                GenderEnum gender = profile == null ? null : profile.getGender();
                memberSnapshots.add(new MatchmakingMember(
                        member.getUserId(),
                        member.getCourtAbility() == com.rally.domain.tournament.enums.CourtAbilityEnum.CAN_BOOK,
                        gender == null ? null : gender.name()));
            }
            LocalDateTime joinedTime = members.stream().map(TournamentEntryData::getCreateTime).filter(java.util.Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            MatchmakingCandidate candidate = new MatchmakingCandidate(
                    item.getKey(),
                    TournamentMatchRound.valueOf(tournament.getCurrentRound().name()),
                    TournamentEntryStatus.WAITING,
                    memberSnapshots,
                    tournament.getMatchType() == MatchTypeEnum.DOUBLE ? 2 : 1,
                    commonValues(members, TournamentEntryData::getAvailableTimes),
                    commonValues(members, TournamentEntryData::getPreferredDistricts),
                    joinedTime);
            candidates.add(new CandidateContext(candidate, List.copyOf(members)));
        }
        return candidates.stream().sorted(Comparator
                .comparing((CandidateContext context) -> context.candidate().joinedTime(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(context -> context.candidate().entryNo())).toList();
    }

    private Set<String> commonValues(
            List<TournamentEntryData> members,
            java.util.function.Function<TournamentEntryData, List<String>> extractor) {
        Set<String> common = null;
        for (TournamentEntryData member : members) {
            List<String> values = extractor.apply(member);
            Set<String> memberValues = values == null ? Set.of() : new HashSet<>(values);
            if (common == null) {
                common = new HashSet<>(memberValues);
            } else {
                common.retainAll(memberValues);
            }
        }
        return common == null ? Set.of() : Set.copyOf(common);
    }

    private Set<CompletedPairing> completedPairings(String tournamentId, TournamentRoundEnum round) {
        List<TournamentMatchData> matches = tournamentMatchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> match.getRound() == round)
                .filter(match -> match.getStatus() == TournamentMatchStatusEnum.COMPLETED)
                .toList();
        if (matches.isEmpty()) return Set.of();
        Map<String, List<MatchParticipantData>> participants = tournamentMatchRepository.findParticipantsByMatchIds(
                        matches.stream().map(TournamentMatchData::getBizId).toList()).stream()
                .collect(Collectors.groupingBy(MatchParticipantData::getMatchId));
        Set<CompletedPairing> result = new HashSet<>();
        for (TournamentMatchData match : matches) {
            List<Integer> entryNos = participants.getOrDefault(match.getBizId(), List.of()).stream()
                    .map(MatchParticipantData::getEntryNo).distinct().sorted().toList();
            for (int i = 0; i < entryNos.size(); i++) {
                for (int j = i + 1; j < entryNos.size(); j++) {
                    result.add(new CompletedPairing(entryNos.get(i), entryNos.get(j)));
                }
            }
        }
        return result;
    }

    private List<TournamentEntryData> entriesForGroup(
            MatchmakingGroup group,
            Map<Integer, CandidateContext> contextByEntryNo) {
        return group.entryNos().stream()
                .map(contextByEntryNo::get)
                .flatMap(context -> context.entries().stream())
                .toList();
    }

    private RuntimeException matchmakingRejection(
            MatchmakingRejection rejection,
            List<List<Integer>> manualGroups,
            Set<Integer> excluded,
            Map<Integer, CandidateContext> candidates) {
        if (rejection == MatchmakingRejection.MANUAL_GROUP_INVALID
                && manualGroups != null
                && manualGroups.stream().filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .anyMatch(entryNo -> entryNo == null
                        || excluded.contains(entryNo)
                        || !candidates.containsKey(entryNo))) {
            return new com.rally.domain.auth.exception.BusinessException(
                    BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        }
        return new com.rally.domain.auth.exception.BusinessException(BizErrorCode.PARAM_ERROR);
    }

    private record CandidateContext(
            MatchmakingCandidate candidate,
            List<TournamentEntryData> entries) {
    }
}
