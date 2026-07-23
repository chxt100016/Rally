package com.rally.wechat.tournament;

import com.rally.web.tournament.TournamentMatchController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/tournament/match")
public class WechatTournamentMatchController extends TournamentMatchController {
}
