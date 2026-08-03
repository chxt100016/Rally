package com.rally.web.config;

import com.rally.config.AuthInterceptor;
import com.rally.config.LogInterceptor;
import com.rally.config.UserBehaviorInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Resource
    private LogInterceptor logInterceptor;

    @Resource
    private UserBehaviorInterceptor userBehaviorInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userBehaviorInterceptor)
                .addPathPatterns("/wechat/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/wechat/**")
                .excludePathPatterns(
                        "/wechat/auth/login",
                        "/actuator/**",
                        "/tour/collect/**",

                        "/wechat/tour/tournament/tournaments",
                        "/wechat/tour/match/upcoming",
                        "/wechat/tour/match/finished",
                        "/wechat/tour/player/players",
                        "/wechat/tour/player/tournament",


                        "/wechat/meetup/list"

                );
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");
    }
}
