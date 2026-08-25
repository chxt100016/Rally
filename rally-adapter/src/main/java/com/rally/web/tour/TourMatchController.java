package com.rally.web.tour;

import com.rally.domain.tour.model.Result;
import com.rally.domain.tour.model.TourMatchDTO;
import com.rally.tour.TourMatchAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tour/match")
public class TourMatchController {

    @Resource
    private TourMatchAppService tourMatchAppService;

    @GetMapping("/upcoming")
    public Result<TourMatchDTO> upcoming(@RequestParam("tournamentIds") List<String> tournamentIds) {
        return Result.ok(tourMatchAppService.upcoming(tournamentIds));
    }

    @GetMapping("/finished")
    public Result<TourMatchDTO> finished(@RequestParam("tournamentIds") List<String> tournamentIds) {
        return Result.ok(tourMatchAppService.finished(tournamentIds));
    }
}
