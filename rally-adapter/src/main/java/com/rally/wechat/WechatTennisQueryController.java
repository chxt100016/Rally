package com.rally.wechat;

import com.rally.domain.tennis.model.MatchQueryResponse;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.tennis.model.TournamentDTO;
import com.rally.tennis.TennisQueryService;
import com.rally.web.tennis.TennisQueryController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wechat/query")
public class WechatTennisQueryController extends TennisQueryController {


}
