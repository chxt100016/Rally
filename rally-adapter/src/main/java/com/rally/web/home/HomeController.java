package com.rally.web.home;

import com.rally.config.OptionalAuth;
import com.rally.home.HomeAppService;
import com.rally.home.model.HomePageDTO;
import com.rally.domain.tour.model.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Resource
    private HomeAppService homeAppService;

    @GetMapping("/page")
    @OptionalAuth
    public Result<HomePageDTO> getHomePage(@RequestParam(value = "cityCode", required = false) String cityCode) {
        return Result.ok(homeAppService.getHomePage(cityCode));
    }
}
