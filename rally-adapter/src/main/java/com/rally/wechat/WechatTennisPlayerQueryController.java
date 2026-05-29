package com.rally.wechat;

import com.rally.domain.tennis.model.PlayerQueryVO;
import com.rally.domain.tennis.model.Result;
import com.rally.tennis.TennisPlayerQueryService;
import com.rally.web.tennis.TennisPlayerQueryController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wechat/query/player")
public class WechatTennisPlayerQueryController extends TennisPlayerQueryController {

}
