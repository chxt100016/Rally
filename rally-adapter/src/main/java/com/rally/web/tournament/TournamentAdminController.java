package com.rally.web.tournament;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.*;
import com.rally.tournament.TournamentAdminAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 赛事管理（运营后台）接口：创建/编辑/激活/废弃/列表
 */
@RestController
@RequestMapping("/tournament/admin")
public class TournamentAdminController {

    @Resource
    private TournamentAdminAppService tournamentAdminAppService;

    /**
     * 创建赛事草稿
     */
    @PostMapping("/create")
    public Result<TournamentIdDTO> create(@Valid @RequestBody TournamentCreateCmd cmd) {
        return Result.ok(tournamentAdminAppService.create(cmd));
    }

    /**
     * 编辑赛事配置
     */
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody TournamentUpdateCmd cmd) {
        tournamentAdminAppService.update(cmd);
        return Result.ok();
    }

    /**
     * 激活赛事
     */
    @PostMapping("/activate")
    public Result<Void> activate(@Valid @RequestBody TournamentActivateCmd cmd) {
        tournamentAdminAppService.activate(cmd);
        return Result.ok();
    }

    /**
     * 废弃赛事
     */
    @PostMapping("/abandon")
    public Result<Void> abandon(@Valid @RequestBody TournamentAbandonCmd cmd) {
        tournamentAdminAppService.abandon(cmd);
        return Result.ok();
    }

    /** 创建赛事线下赛活动。 */
    @PostMapping("/offline/meetup/create")
    public Result<String> createOfflineMeetup(@Valid @RequestBody TournamentOfflineMeetupCmd cmd) {
        return Result.ok(tournamentAdminAppService.createOfflineMeetup(cmd));
    }

    /**
     * 手动执行赛事匹配；不传参数时扫描全部到时赛事，传 tournamentId 时只处理指定赛事。
     */
    @PostMapping("/match/run")
    public Result<Void> runTournamentMatch(@RequestBody(required = false) TournamentMatchRunCmd cmd) {
        if (cmd != null && cmd.getManualGroups() != null && !cmd.getManualGroups().isEmpty()) {
            tournamentAdminAppService.runTournamentMatch(cmd.getTournamentId(), cmd.getManualGroups(), cmd.getExcludedEntryNos());
        } else if (cmd != null && cmd.getExcludedEntryNos() != null && !cmd.getExcludedEntryNos().isEmpty()) {
            tournamentAdminAppService.runTournamentMatchWithExcludedEntries(cmd.getTournamentId(), cmd.getExcludedEntryNos());
        } else if (cmd != null && StringUtils.isNotBlank(cmd.getTournamentId())) {
            tournamentAdminAppService.runTournamentMatchWithExcludedEntries(cmd.getTournamentId(), null);
        } else {
            tournamentAdminAppService.runTournamentMatch();
        }
        return Result.ok();
    }

    /** 取消该赛事所有尚未提交订场信息的比赛，并使参赛队重新回到 WAITING 匹配池。 */
    @PostMapping("/match/cancel")
    public Result<Void> cancelTournamentMatch(@Valid @RequestBody TournamentMatchCancelCmd cmd) {
        tournamentAdminAppService.cancelUnsubmittedTournamentMatches(cmd.getTournamentId());
        return Result.ok();
    }

    /** 终止指定赛事中指定序号的一场未完成比赛。 */
    @PostMapping("/match/cancel/single")
    public Result<Void> cancelSingleTournamentMatch(
            @Valid @RequestBody TournamentSingleMatchCancelCmd cmd) {
        tournamentAdminAppService.cancelSingleTournamentMatch(cmd);
        return Result.ok();
    }

    /** 按赛事编号和比赛序号一次性代确认全部未确认参与者赛约。 */
    @PostMapping("/match/booking/confirm")
    public Result<Void> confirmBookingByAdmin(
            @Valid @RequestBody TournamentBookingConfirmAdminCmd cmd) {
        tournamentAdminAppService.confirmBookingByAdmin(cmd);
        return Result.ok();
    }

    /** 按赛事编号和比赛序号一次性代确认全部待确认参与者赛果。 */
    @PostMapping("/match/result/confirm")
    public Result<Void> confirmResultByAdmin(
            @Valid @RequestBody TournamentResultConfirmAdminCmd cmd) {
        tournamentAdminAppService.confirmResultByAdmin(cmd);
        return Result.ok();
    }

    /** 淘汰指定赛事当前轮次中未进入比赛的单个用户。 */
    @PostMapping("/entry/eliminate-unmatched")
    public Result<Void> eliminateUnmatchedEntries(
            @Valid @RequestBody TournamentUnmatchedEntryEliminationCmd cmd) {
        tournamentAdminAppService.eliminateUnmatchedEntries(cmd);
        return Result.ok();
    }

    /** 将指定用户处于等待匹配状态的赛事报名冻结。 */
    @PostMapping("/entry/freeze")
    public Result<Void> freezeEntry(@Valid @RequestBody TournamentEntryFreezeCmd cmd) {
        tournamentAdminAppService.freezeEntry(cmd);
        return Result.ok();
    }

    /**
     * 后台赛事列表
     */
    @PostMapping("/list")
    public Result<PageDTO<TournamentAdminItemDTO>> list(@Valid @RequestBody TournamentAdminListCmd cmd) {
        return Result.ok(tournamentAdminAppService.list(cmd));
    }
}
