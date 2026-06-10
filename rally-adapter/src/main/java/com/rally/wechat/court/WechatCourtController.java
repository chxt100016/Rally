package com.rally.wechat.court;

import com.rally.web.court.CourtController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信端球场接口
 */
@RestController
@RequestMapping("/wechat/court")
public class WechatCourtController extends CourtController {
}
