package com.rally.wechat.tournament;


import com.rally.web.tournament.TournamentDetailController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛事落地页详情（聚合查询）接口：只读，可匿名访问
 */
@RestController
@RequestMapping("/wechat/tournament/detail")
public class WechatTournamentDetailController extends TournamentDetailController {


}
