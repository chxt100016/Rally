package com.rally.web.tournament;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.TournamentCommentDTO;
import com.rally.domain.tournament.model.TournamentCommentPublishCmd;
import com.rally.domain.tournament.model.TournamentCommentListDTO;
import com.rally.tournament.TournamentCommentAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛事评论接口。
 */
@RestController
@RequestMapping("/tournament/comment")
public class TournamentCommentController {

    @Resource
    private TournamentCommentAppService tournamentCommentAppService;

    @PostMapping("/publish")
    public Result<TournamentCommentDTO> publish(@Valid @RequestBody TournamentCommentPublishCmd cmd) {
        return Result.ok(tournamentCommentAppService.publish(cmd));
    }

    @GetMapping("/list")
    public Result<TournamentCommentListDTO> list(
            @RequestParam("tournamentId") String tournamentId,
            @RequestParam(value = "beforeCommentId", required = false) String beforeCommentId,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.ok(tournamentCommentAppService.list(tournamentId, beforeCommentId, limit));
    }
}
