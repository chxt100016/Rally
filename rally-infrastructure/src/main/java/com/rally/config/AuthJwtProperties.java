package com.rally.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthJwtProperties {
    private String secret;
    private int expireDays = 7;
}
