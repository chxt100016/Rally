package com.rally.web.tennis;

import com.rally.domain.tennis.model.PlayerQueryVO;
import com.rally.domain.tennis.model.Result;
import com.rally.tennis.TennisPlayerQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/query/player")
public class TennisPlayerQueryController {

    @Resource
    private TennisPlayerQueryService tennisPlayerQueryService;


    @GetMapping("/players")
    public Result<List<PlayerQueryVO>> players(@RequestParam("tour") String tour) {
        List<PlayerQueryVO> data = tennisPlayerQueryService.queryPlayers(tour);
        return Result.ok(data);
    }
}
