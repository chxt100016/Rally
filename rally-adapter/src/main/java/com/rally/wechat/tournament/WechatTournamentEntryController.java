package com.rally.wechat.tournament;

import com.rally.web.tournament.TournamentEntryController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛事报名（用户端）接口：报名/修改偏好/退出
 */
@RestController
@RequestMapping("/wechat/tournament/entry")
public class WechatTournamentEntryController extends TournamentEntryController {

}
