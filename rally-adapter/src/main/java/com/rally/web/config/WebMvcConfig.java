package com.rally.web.config;

import com.rally.config.AuthInterceptor;
import com.rally.config.ClientChannelInterceptor;
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

    @Resource
    private ClientChannelInterceptor clientChannelInterceptor;

    @Resource
    private AdminApiKeyInterceptor adminApiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientChannelInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(userBehaviorInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(adminApiKeyInterceptor)
                .addPathPatterns(
                        "/tournament/admin/**",
                        "/system/admin/**",
                        "/court/admin/**"
                );
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/wechat/auth/login",
                        "/wechat/pay/notify",
                        "/actuator/**",
                        "/error",
                        "/test/**",
                        "/tour/collect/**",
                        "/tournament/admin/**",
                        "/system/admin/**",
                        "/court/admin/**",

                        "/tour/tournament/tournaments",
                        "/tour/match/upcoming",
                        "/tour/match/finished",
                        "/tour/player/players",
                        "/tour/player/tournament",

                        "/meetup/list"

                );
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");
    }
}
