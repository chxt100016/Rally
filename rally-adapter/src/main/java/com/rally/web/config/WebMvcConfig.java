package com.rally.web.config;

import com.rally.web.auth.AuthInterceptor;
import com.rally.web.auth.LogInterceptor;
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
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/wechat/**")
                .excludePathPatterns(
                        "/wechat/auth/login", "/actuator/**",
                        "/wechat/query/tournaments",
                        "/wechat/query/matches",
                        "/wechat/query/player/players",
                        "/tennis/collect/**",
                        "/wechat/user/video/callback"
                );
    }
}
