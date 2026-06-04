package com.rally.wechat.tennis;

import com.rally.web.tennis.TennisPlayerQueryController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/query/player")
public class WechatTennisPlayerQueryController extends TennisPlayerQueryController {

}
