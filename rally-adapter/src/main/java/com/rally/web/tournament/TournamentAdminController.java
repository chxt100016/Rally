package com.rally.web.tournament;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.*;
import com.rally.tournament.TournamentAdminAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
     * 编辑草稿
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

    /**
     * 后台赛事列表
     */
    @PostMapping("/list")
    public Result<PageDTO<TournamentAdminItemDTO>> list(@Valid @RequestBody TournamentAdminListCmd cmd) {
        return Result.ok(tournamentAdminAppService.list(cmd));
    }
}
