package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.tournament.model.TournamentMatchTeam;
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
    private final TournamentMatchingService tournamentMatchingService;
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
        List<TournamentMatchTeam> teams = waitingTeams(tournament, excludedEntryNos);
        int groupSize = groupSize(tournament);
        List<MatchGroup> groups = tournamentMatchingService.group(teams, groupSize, playedPairs(tournamentId, tournament.getCurrentRound()));
        return tournamentMatchAssembleService.assemble(tournamentId, groups, tournament.getCurrentRound(), groupSize);
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
        int groupSize = groupSize(tournament);
        Map<Integer, TournamentMatchTeam> teams = waitingTeams(tournament, excludedEntryNos).stream()
                .collect(Collectors.toMap(TournamentMatchTeam::getEntryNo, team -> team));
        Set<Integer> usedEntryNos = new HashSet<>();
        List<MatchGroup> groups = new ArrayList<>();
        for (List<Integer> entryNos : manualGroups) {
            Assert.isTrue(entryNos != null && entryNos.size() == groupSize, BizErrorCode.PARAM_ERROR);
            List<TournamentMatchTeam> groupTeams = new ArrayList<>();
            for (Integer entryNo : entryNos) {
                Assert.isTrue(entryNo != null && usedEntryNos.add(entryNo), BizErrorCode.PARAM_ERROR);
                TournamentMatchTeam team = teams.get(entryNo);
                Assert.notNull(team, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
                groupTeams.add(team);
            }
            groups.add(new MatchGroup(groupTeams.stream().flatMap(team -> team.getEntries().stream()).toList()));
        }
        List<TournamentMatch> matches = new ArrayList<>(
                tournamentMatchAssembleService.assemble(tournamentId, groups, tournament.getCurrentRound(), groupSize));
        // 手工指定的队伍已在上一句推进为 IN_MATCH；重新查询即可只对剩余 WAITING 队伍自动匹配。
        matches.addAll(matchCurrentRound(tournamentId, excludedEntryNos));
        return matches;
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

    private List<TournamentMatchTeam> waitingTeams(TournamentData tournament, List<Integer> excludedEntryNos) {
        Set<Integer> excluded = excludedEntryNos == null ? Set.of() : new HashSet<>(excludedEntryNos);
        List<TournamentEntryData> entries = tournamentEntryRepository.findByTournamentId(tournament.getBizId()).stream()
                .filter(entry -> entry.getStatus() == TournamentEntryStatusEnum.WAITING)
                .filter(entry -> entry.getCurrentRound() == tournament.getCurrentRound())
                .filter(entry -> !excluded.contains(entry.getEntryNo()))
                .toList();
        Map<Integer, List<TournamentEntryData>> byEntryNo = entries.stream().collect(Collectors.groupingBy(TournamentEntryData::getEntryNo));
        List<TournamentMatchTeam> teams = new ArrayList<>();
        for (Map.Entry<Integer, List<TournamentEntryData>> item : byEntryNo.entrySet()) {
            List<TournamentEntryData> members = item.getValue();
            // 双打队伍尚未由两位成员完成报名时，不能参加匹配。
            if (tournament.getMatchType() == MatchTypeEnum.DOUBLE && members.size() != 2) {
                continue;
            }
            Set<String> districts = members.stream().flatMap(entry -> entry.getPreferredDistricts() == null ? java.util.stream.Stream.<String>empty() : entry.getPreferredDistricts().stream())
                    .collect(Collectors.toSet());
            List<GenderEnum> genders = userProfileRepository.findByUserIds(members.stream().map(TournamentEntryData::getUserId).toList())
                    .stream().map(profile -> profile == null ? null : profile.getGender()).toList();
            LocalDateTime joinedTime = members.stream().map(TournamentEntryData::getCreateTime).filter(java.util.Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            teams.add(new TournamentMatchTeam(item.getKey(), members, districts, genders, joinedTime));
        }
        return teams.stream().sorted(Comparator.comparing(TournamentMatchTeam::getJoinedTime,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TournamentMatchTeam::getEntryNo)).toList();
    }

    private Set<String> playedPairs(String tournamentId, TournamentRoundEnum round) {
        List<TournamentMatchData> matches = tournamentMatchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> match.getRound() == round)
                .filter(match -> match.getStatus() == TournamentMatchStatusEnum.COMPLETED)
                .toList();
        if (matches.isEmpty()) return Set.of();
        Map<String, List<MatchParticipantData>> participants = tournamentMatchRepository.findParticipantsByMatchIds(
                        matches.stream().map(TournamentMatchData::getBizId).toList()).stream()
                .collect(Collectors.groupingBy(MatchParticipantData::getMatchId));
        Set<String> result = new HashSet<>();
        for (TournamentMatchData match : matches) {
            List<Integer> entryNos = participants.getOrDefault(match.getBizId(), List.of()).stream()
                    .map(MatchParticipantData::getEntryNo).distinct().sorted().toList();
            for (int i = 0; i < entryNos.size(); i++) {
                for (int j = i + 1; j < entryNos.size(); j++) result.add(pairKey(entryNos.get(i), entryNos.get(j)));
            }
        }
        return result;
    }

    private String pairKey(Integer left, Integer right) {
        return left < right ? left + "|" + right : right + "|" + left;
    }
}
