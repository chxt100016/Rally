package com.rally.web.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.tennis.TennisCollectFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    @GetMapping("/draws")
    public void draw(@RequestParam("tournamentId") String tournamentId) {
        tennisCollectFacade.draws(tournamentId);
    }

    @GetMapping("/currentDraws")
    public String collectCurrentDraws() {
        tennisCollectFacade.currentDraws();
        return "当前签表采集完成";
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

    @GetMapping("/completed")
    public String completed(@RequestParam("tournamentId") String tournamentId,
                            @RequestParam("year") Integer year) {
        TennisTournamentPO tournament = new TennisTournamentPO();
        tournament.setTournamentId(tournamentId);
        tournament.setYear(year);
        tennisCollectFacade.completed(tournament);
        return "已完成比赛采集完成";
    }

    /**
     * 测试接口：查询所有用户并生成token
     */
    @GetMapping("/tokens")
    public Map<String, String> getAllUserTokens() {
        return tennisCollectFacade.getAllUserTokens();
    }
}
