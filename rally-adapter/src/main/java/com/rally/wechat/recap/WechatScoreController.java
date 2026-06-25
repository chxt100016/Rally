package com.rally.wechat.recap;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreListQueryCmd;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.tour.model.Result;
import com.rally.recap.ScoreAppService;
import com.rally.web.recap.ScoreController;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat/recap/score")
public class WechatScoreController extends ScoreController {


}
