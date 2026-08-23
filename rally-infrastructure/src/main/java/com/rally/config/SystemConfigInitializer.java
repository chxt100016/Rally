package com.rally.config;

import com.rally.domain.system.SystemConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 初始化数据库系统配置缓存。 */
@Component
@RequiredArgsConstructor
public class SystemConfigInitializer {

    private final SysConfigLoaderImpl sysConfigLoader;

    @PostConstruct
    public void initialize() {
        SystemConfig.setSysConfigLoader(sysConfigLoader);
        SystemConfig.init();
    }
}
