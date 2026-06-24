package com.rally.web.tour;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tour.model.TournamentDTO;
import com.rally.tour.TournamentQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tour/tournament")
public class TourQueryController {

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
