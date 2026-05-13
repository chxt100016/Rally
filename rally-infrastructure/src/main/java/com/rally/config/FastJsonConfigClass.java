package com.rally.config;


import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
public class FastJsonConfigClass {

    @Bean
    public HttpMessageConverter<?> fastJsonHttpMessageConverter() {

        FastJsonHttpMessageConverter converter =
                new FastJsonHttpMessageConverter();

        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        converter.setFastJsonConfig(config);

        return converter;
    }
}