package com.rally.tournament.currentroundmatching.activity;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchRunCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentBatchMatchService;
import com.rally.notify.NotificationEventId;
import com.rally.notify.TournamentNotifyAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务活动 run-current-round-matching：编排赛事当前轮次并建立比赛。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunCurrentRoundMatchingActivity {

    private final TournamentAdminService tournamentAdminService;
    private final TournamentBatchMatchService tournamentBatchMatchService;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 处理运营手工入口。空请求表示全量扫描，其余参数组合与既有 HTTP 入口保持一致。
     */
    public synchronized void execute(TournamentMatchRunCmd command) {
        if (command != null && command.getManualGroups() != null
                && !command.getManualGroups().isEmpty()) {
            matchManually(command.getTournamentId(), command.getManualGroups(),
                    command.getExcludedEntryNos());
        } else if (command != null && command.getExcludedEntryNos() != null
                && !command.getExcludedEntryNos().isEmpty()) {
            matchSpecifiedTournament(command.getTournamentId(), command.getExcludedEntryNos());
        } else if (command != null && StringUtils.isNotBlank(command.getTournamentId())) {
            matchSpecifiedTournament(command.getTournamentId(), null);
        } else {
            scanEligibleTournaments();
        }
    }

    /**
     * 处理每日定时入口；每个赛事独立事务，单个赛事失败不中断扫描。
     */
    public synchronized void executeScheduled() {
        scanEligibleTournaments();
    }

    private void scanEligibleTournaments() {
        // A1：仅选择已到资格赛开始时间的 ACTIVE 赛事，候选轮次由批量服务冻结。
        List<TournamentData> tournaments = tournamentBatchMatchService
                .listTournamentsToMatch(LocalDateTime.now());
        for (TournamentData tournament : tournaments) {
            try {
                List<TournamentMatch> matches = tournamentBatchMatchService
                        .matchCurrentRound(tournament.getBizId());
                notifyMatched(tournament, matches);
            } catch (Exception exception) {
                log.error("赛事匹配失败 tournamentId={}", tournament.getBizId(), exception);
            }
        }
    }

    private void matchManually(
            String tournamentId,
            List<List<Integer>> manualGroups,
            List<Integer> excludedEntryNos) {
        TournamentData tournament = tournamentAdminService.get(tournamentId).getData();
        /*
         * A2-A4：批量服务在同一赛事事务中校验手工组，先落地手工组，
         * 再对剩余 WAITING 队伍执行自动优化并创建比赛、锁定报名。
         */
        List<TournamentMatch> matches = tournamentBatchMatchService
                .matchCurrentRoundManually(tournamentId, manualGroups, excludedEntryNos);
        notifyMatched(tournament, matches);
    }

    private void matchSpecifiedTournament(String tournamentId, List<Integer> excludedEntryNos) {
        TournamentData tournament = tournamentAdminService.get(tournamentId).getData();
        // A1-A4：候选滤除、全局最优分组、序号分配和报名锁定均由单赛事事务完成。
        List<TournamentMatch> matches = tournamentBatchMatchService
                .matchCurrentRound(tournamentId, excludedEntryNos);
        notifyMatched(tournament, matches);
    }

    private void notifyMatched(TournamentData tournament, List<TournamentMatch> matches) {
        // A5：稳定事件标识与参赛者去重保持通知幂等；无新比赛时正常返回。
        for (TournamentMatch match : matches) {
            List<String> userIds = match.getParticipants().stream()
                    .map(MatchParticipantData::getUserId)
                    .distinct()
                    .toList();
            notificationDeliveryService.notify(
                    NotificationEventId.of(NoticeScene.TOURNAMENT_MATCHED, match.getMatchId()),
                    NotifyBizType.TOURNAMENT,
                    tournament.getBizId(),
                    NoticeScene.TOURNAMENT_MATCHED,
                    userIds,
                    TournamentNotifyAssembler.matchedData(tournament.getTournamentName()));
        }
    }
}
