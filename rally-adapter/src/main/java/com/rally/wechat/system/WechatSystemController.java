package com.rally.wechat.system;

import com.rally.web.system.SystemController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信渠道 - 系统配置查询接口。
 * 复用 web 层的通用实现。
 */
@RestController
@RequestMapping("/wechat/system")
public class WechatSystemController extends SystemController {


}
