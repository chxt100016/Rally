package com.rally.web.tennis;

import com.rally.tennis.TennisCollectFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/tennis/collect")
public class TennisCollectController {

    @Resource
    private TennisCollectFacade tennisCollectFacade;

    @GetMapping("/tournaments")
    public void tournaments(@RequestParam("year") Integer year) {
        tennisCollectFacade.tournaments(year);
    }


    @GetMapping("/currentDraws")
    public String collectCurrentDraws() {
        tennisCollectFacade.currentDraws();
        return "当前签表采集完成";
    }

    @GetMapping("/draws")
    public String draws(@RequestParam("tour") String tour, @RequestParam("tournamentId") String tournamentId, @RequestParam("year") int year) {
        tennisCollectFacade.draws(tour, tournamentId, year);
        return "签表采集完成: " + tournamentId + "/" + year;
    }

    @GetMapping("/oop")
    public String oop() {
        tennisCollectFacade.oop();
        return "比赛详情采集完成";
    }

    @GetMapping("/live")
    public void live() {
        tennisCollectFacade.liveMatch();
    }

    @GetMapping("/rank")
    public String rank() {
        tennisCollectFacade.rank();
        return "排名采集完成";
    }
}
