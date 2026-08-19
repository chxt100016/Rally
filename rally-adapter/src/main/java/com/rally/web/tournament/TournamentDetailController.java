package com.rally.web.tournament;

import com.rally.config.OptionalAuth;
import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.tournament.TournamentDetailAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛事落地页详情（聚合查询）接口：可匿名访问，已报名用户访问时记录最近访问时间
 */
@RestController
@RequestMapping({"/tournament/detail", "/wechat/tournament/detail"})
public class TournamentDetailController {

    @Resource
    private TournamentDetailAppService tournamentDetailAppService;

    /**
     * 落地页核心聚合接口
     */
    @OptionalAuth
    @GetMapping("/{bizId}")
    public Result<TournamentDetailDTO> detail(@PathVariable("bizId") String bizId) {
        return Result.ok(tournamentDetailAppService.detail(bizId));
    }
}
