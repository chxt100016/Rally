package com.rally.wechat.tour;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tour.model.TournamentDTO;
import com.rally.tour.TournamentQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wechat/tour/tournament")
public class WechatTourQueryController {

    @Resource
    private TournamentQueryService tournamentQueryService;

    @GetMapping("/tournaments")
    public Result<List<TournamentDTO>> tournaments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "range", required = false) String range) {
        List<TournamentDTO> data = tournamentQueryService.queryTournaments(status, type, range);
        return Result.ok(data);
    }
}
