package com.rally.web.config;

import com.rally.config.AuthInterceptor;
import com.rally.config.LogInterceptor;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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
                        "/wechat/tour/player/tournament"

                );
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");
    }
}
