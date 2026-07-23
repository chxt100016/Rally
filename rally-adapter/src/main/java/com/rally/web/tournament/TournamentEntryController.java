package com.rally.web.tournament;

import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.tournament.model.TournamentEntryPayCmd;
import com.rally.domain.tournament.model.TournamentEntryUpdateCmd;
import com.rally.domain.tournament.model.TournamentJoinCmd;
import com.rally.domain.tournament.model.TournamentWithdrawCmd;
import com.rally.domain.tournament.model.TournamentWithdrawResultDTO;
import com.rally.tournament.TournamentEntryAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 赛事报名（用户端）接口：报名/修改偏好/退出
 */
@RestController
@RequestMapping("/tournament/entry")
public class TournamentEntryController {

    @Resource
    private TournamentEntryAppService tournamentEntryAppService;

    /**
     * 报名
     */
    @PostMapping("/join")
    public Result<TournamentEntryDTO> join(@Valid @RequestBody TournamentJoinCmd cmd) {
        return Result.ok(tournamentEntryAppService.join(cmd));
    }

    /**
     * 修改报名偏好
     */
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody TournamentEntryUpdateCmd cmd) {
        tournamentEntryAppService.update(cmd);
        return Result.ok();
    }

    /**
     * 支付报名费，锁定正赛席位
     */
    @PostMapping("/pay")
    public Result<PrepayResult> pay(@Valid @RequestBody TournamentEntryPayCmd cmd) {
        return Result.ok(tournamentEntryAppService.pay(cmd));
    }

    /**
     * 退出赛事
     */
    @PostMapping("/withdraw")
    public Result<TournamentWithdrawResultDTO> withdraw(@Valid @RequestBody TournamentWithdrawCmd cmd) {
        return Result.ok(tournamentEntryAppService.withdraw(cmd));
    }
}
