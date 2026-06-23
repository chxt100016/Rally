package com.rally.web.tour;

import com.rally.db.user.entity.UserPO;
import com.rally.domain.tour.model.TournamentData;
import com.rally.db.user.mapper.UserMapper;
import com.rally.tour.TourCollectFacade;
import com.rally.utils.TokenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/tour/collect")
public class TourCollectController {

    @Resource
    private TourCollectFacade tourCollectFacade;

    @GetMapping("/tournaments")
    public void tournaments(@RequestParam("year") Integer year) {
        tourCollectFacade.tournaments(year);
    }

    @GetMapping("/draws")
    public void draw(@RequestParam("tournamentId") String tournamentId) {
        tourCollectFacade.draws(tournamentId);
    }

    @GetMapping("/currentDraws")
    public String collectCurrentDraws() {
        tourCollectFacade.currentDraws();
        return "当前签表采集完成";
    }


    @GetMapping("/oop")
    public String oop() {
        tourCollectFacade.oop();
        return "比赛详情采集完成";
    }

    @GetMapping("/live")
    public void live() {
        tourCollectFacade.liveMatch();
    }

    @GetMapping("/rank")
    public String rank() {
        tourCollectFacade.rank();
        return "排名采集完成";
    }

    @GetMapping("/completed")
    public String completed(@RequestParam("tournamentId") String tournamentId,
                            @RequestParam("year") Integer year) {
        TournamentData tournament = new TournamentData();
        tournament.setTournamentId(tournamentId);
        tournament.setYear(year);
        tourCollectFacade.completed(tournament);
        return "已完成比赛采集完成";
    }

}
