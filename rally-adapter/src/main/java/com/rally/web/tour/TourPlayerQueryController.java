package com.rally.web.tour;

import com.rally.domain.tour.model.PlayerQueryVO;
import com.rally.domain.tour.model.PlayerTournamentVO;
import com.rally.domain.tour.model.Result;
import com.rally.tour.PlayerTournamentQueryService;
import com.rally.tour.TourPlayerQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tour/player")
public class TourPlayerQueryController {

    @Resource
    private TourPlayerQueryService tourPlayerQueryService;

    @Resource
    private PlayerTournamentQueryService playerTournamentQueryService;

    @GetMapping("/players")
    public Result<List<PlayerQueryVO>> players(@RequestParam("tour") String tour) {
        List<PlayerQueryVO> data = tourPlayerQueryService.queryPlayers(tour);
        return Result.ok(data);
    }

    @GetMapping("/tournament")
    public Result<PlayerTournamentVO> tournament(
            @RequestParam("tournamentId") String tournamentId,
            @RequestParam("year") Integer year,
            @RequestParam("playerId") String playerId,
            @RequestParam("drawType") String drawType) {
        PlayerTournamentVO data = playerTournamentQueryService.query(tournamentId, year, playerId, drawType);
        return Result.ok(data);
    }
}
