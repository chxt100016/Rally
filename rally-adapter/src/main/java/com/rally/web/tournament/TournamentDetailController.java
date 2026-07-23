package com.rally.web.tournament;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.tournament.TournamentDetailAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛事落地页详情（聚合查询）接口：只读，可匿名访问
 */
@RestController
@RequestMapping("/tournament/detail")
public class TournamentDetailController {

    @Resource
    private TournamentDetailAppService tournamentDetailAppService;

    /**
     * 落地页核心聚合接口
     */
    @GetMapping("/{bizId}")
    public Result<TournamentDetailDTO> detail(@PathVariable("bizId") String bizId) {
        return Result.ok(tournamentDetailAppService.detail(bizId));
    }
}
