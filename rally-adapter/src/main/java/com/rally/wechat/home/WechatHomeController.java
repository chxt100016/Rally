package com.rally.wechat.home;

import com.rally.config.OptionalAuth;
import com.rally.domain.tour.model.Result;
import com.rally.home.HomeAppService;
import com.rally.home.model.HomePageDTO;
import com.rally.web.home.HomeController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/home")
public class WechatHomeController extends HomeController {

}
