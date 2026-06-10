package com.rally.domain.system.gateway;

import com.rally.domain.system.model.Location;

import java.util.Map;

/**
 * 系统配置加载网关。
 * 由 infrastructure 层实现，启动时从 DB 全量加载配置到内存。
 */
public interface SysConfigLoader {

    /**
     * 全量加载所有启用的配置项。
     * @return key = "scope|configKey", value = configValue
     */
    Map<String, String> loadAll();


    Map<String, Location> city();

    Map<String, Location> district();
}
