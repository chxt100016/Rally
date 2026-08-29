package com.rally.tournament;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tournament.model.*;
import com.rally.tournament.currentroundmatching.activity.RunCurrentRoundMatchingActivity;
import com.rally.tournament.entryfreeze.activity.FreezeEntryActivity;
import com.rally.tournament.offlinemeetupcreate.activity.CreateOfflineMeetupActivity;
import com.rally.tournament.singlematchcancel.activity.CloseCancelledMatchDraftMeetupActivity;
import com.rally.tournament.singlematchcancel.activity.DeleteCancellableMatchActivity;
import com.rally.tournament.singlematchcancel.activity.ReleaseCancelledMatchEntriesActivity;
import com.rally.tournament.tournamentactivate.activity.ActivateTournamentActivity;
import com.rally.tournament.tournamentabandon.activity.AbandonTournamentActivity;
import com.rally.tournament.tournamentconfigupdate.activity.UpdateTournamentConfigActivity;
import com.rally.tournament.tournamentdraftcreate.activity.CreateTournamentDraftActivity;
import com.rally.tournament.tournamentlist.activity.QueryTournamentAdminListActivity;
import com.rally.tournament.unbookedmatchcancel.activity.CancelUnbookedMatchesActivity;
import com.rally.tournament.unmatchedentryelimination.activity.EliminateUnmatchedEntryUnitsActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 赛事管理（运营后台）写流程编排：创建/编辑/激活/废弃/列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentAdminAppService {

    private final CreateTournamentDraftActivity createTournamentDraftActivity;

    private final AbandonTournamentActivity abandonTournamentActivity;

    private final ActivateTournamentActivity activateTournamentActivity;

    private final UpdateTournamentConfigActivity updateTournamentConfigActivity;

    private final CreateOfflineMeetupActivity createOfflineMeetupActivity;

    private final RunCurrentRoundMatchingActivity runCurrentRoundMatchingActivity;

    private final CancelUnbookedMatchesActivity cancelUnbookedMatchesActivity;

    private final DeleteCancellableMatchActivity deleteCancellableMatchActivity;

    private final CloseCancelledMatchDraftMeetupActivity closeCancelledMatchDraftMeetupActivity;

    private final ReleaseCancelledMatchEntriesActivity releaseCancelledMatchEntriesActivity;

    private final EliminateUnmatchedEntryUnitsActivity eliminateUnmatchedEntryUnitsActivity;

    private final FreezeEntryActivity freezeEntryActivity;

    private final QueryTournamentAdminListActivity queryTournamentAdminListActivity;

    /**
     * 创建赛事草稿
     */
    @Transactional
    public TournamentIdDTO create(TournamentCreateCmd cmd) {
        return createTournamentDraftActivity.execute(cmd);
    }

    /**
     * 编辑赛事配置
     */
    @Transactional
    public void update(TournamentUpdateCmd cmd) {
        updateTournamentConfigActivity.execute(cmd);
    }

    /**
     * 激活赛事
     */
    @Transactional
    public void activate(TournamentActivateCmd cmd) {
        activateTournamentActivity.execute(cmd);
    }

    /**
     * 废弃赛事
     */
    @Transactional
    public void abandon(TournamentAbandonCmd cmd) {
        abandonTournamentActivity.execute(cmd);
    }

    /** 创建线下赛活动并自动加入所有达到线下赛轮次的参赛者。 */
    @Transactional
    public String createOfflineMeetup(TournamentOfflineMeetupCmd cmd) {
        return createOfflineMeetupActivity.execute(cmd);
    }

    /**
     * 批量匹配所有已到开始时间的赛事，并在每批比赛落地后触发匹配通知。
     * Job 与运营后台手动接口统一调用此入口。
     */
    public synchronized void runTournamentMatch() {
        runCurrentRoundMatchingActivity.executeScheduled();
    }

    /** 运营手动指定一个赛事当前轮次的分组，校验由领域服务完成。 */
    public synchronized void runTournamentMatch(String tournamentId, List<List<Integer>> manualGroups) {
        runTournamentMatch(tournamentId, manualGroups, null);
    }

    /** 运营指定分组并可临时排除多个 entryNo。 */
    public synchronized void runTournamentMatch(String tournamentId, List<List<Integer>> manualGroups, List<Integer> excludedEntryNos) {
        TournamentMatchRunCmd command = new TournamentMatchRunCmd();
        command.setTournamentId(tournamentId);
        command.setManualGroups(manualGroups);
        command.setExcludedEntryNos(excludedEntryNos);
        runCurrentRoundMatchingActivity.execute(command);
    }

    /** 运营仅匹配指定赛事当前轮次，并可临时排除多个 entryNo。 */
    public synchronized void runTournamentMatchWithExcludedEntries(String tournamentId, List<Integer> excludedEntryNos) {
        TournamentMatchRunCmd command = new TournamentMatchRunCmd();
        command.setTournamentId(tournamentId);
        command.setExcludedEntryNos(excludedEntryNos);
        runCurrentRoundMatchingActivity.execute(command);
    }

    /** 运营批量取消一个赛事中尚未提交订场信息的比赛，并将参赛者退回当前轮次的匹配池。 */
    public void cancelUnsubmittedTournamentMatches(String tournamentId) {
        cancelUnbookedMatchesActivity.execute(tournamentId);
    }

    /** 运营终止一场未完成比赛，并在同一事务中完成赛约和报名联动。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelSingleTournamentMatch(TournamentSingleMatchCancelCmd cmd) {
        var terminationSnapshot = deleteCancellableMatchActivity.execute(
                cmd.getTournamentId(), cmd.getMatchNo());
        terminationSnapshot = closeCancelledMatchDraftMeetupActivity.execute(terminationSnapshot);
        releaseCancelledMatchEntriesActivity.execute(terminationSnapshot);
    }

    /** 运营淘汰当前轮次未进入比赛的指定用户，与匹配入口使用同一实例锁。 */
    public synchronized void eliminateUnmatchedEntries(
            TournamentUnmatchedEntryEliminationCmd cmd) {
        eliminateUnmatchedEntryUnitsActivity.execute(
                cmd.getTournamentId(), cmd.getUserId());
    }

    /** 运营将指定用户处于等待匹配状态的报名冻结。 */
    @Transactional
    public void freezeEntry(TournamentEntryFreezeCmd cmd) {
        freezeEntryActivity.execute(cmd);
    }

    /**
     * 后台赛事列表
     */
    public PageDTO<TournamentAdminItemDTO> list(TournamentAdminListCmd cmd) {
        return queryTournamentAdminListActivity.execute(cmd);
    }
}
