package com.rally.config;

import com.alibaba.fastjson2.JSON;
import com.rally.config.event.ConfigRefreshEvent;
import com.rally.db.config.convert.SysConfigConvertMapper;
import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.repository.SysConfigRepository;
import com.rally.domain.config.enums.ValueType;
import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.config.model.ConfigData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConfigGateway 的 MySQL 实现。
 * 启动时全量加载到 ConcurrentHashMap，支持 ConfigRefreshEvent 刷新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlConfigGatewayImpl implements ConfigGateway, ApplicationListener<ConfigRefreshEvent> {

    private final SysConfigRepository repository;

    // 缓存键 = scope + '|' + config_key，value = 字符串化的 config_value
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public void onApplicationEvent(ConfigRefreshEvent event) {
        reload();
    }

    private void reload() {
        try {
            List<SysConfigPO> allEnabled = repository.findAllEnabled();
            ConcurrentHashMap<String, String> newCache = new ConcurrentHashMap<>();
            for (SysConfigPO po : allEnabled) {
                String cacheKey = po.getScope() + "|" + po.getConfigKey();
                newCache.put(cacheKey, po.getConfigValue());
            }
            cache.clear();
            cache.putAll(newCache);
            log.info("配置缓存加载完成，共 {} 项", cache.size());
        } catch (Exception e) {
            log.error("配置缓存加载失败", e);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        return getString(key, "global", defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key, "global", null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 解析 int 失败，回退默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        String value = getString(key, "global", null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 解析 float 失败，回退默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean getBool(String key, boolean defaultValue) {
        String value = getString(key, "global", null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            log.warn("配置项 {} 解析 bool 失败，回退默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public <T> T getJson(String key, Class<T> cls, T defaultValue) {
        String value = getString(key, "global", null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return JSON.parseObject(value, cls);
        } catch (Exception e) {
            log.warn("配置项 {} 解析 JSON 失败，回退默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public String getString(String key, String scope, String defaultValue) {
        // 先查 scope 专属值
        String cacheKey = scope + "|" + key;
        String value = cache.get(cacheKey);

        // scope 非 global 时，未命中回退 global
        if (value == null && !"global".equals(scope)) {
            cacheKey = "global|" + key;
            value = cache.get(cacheKey);
        }

        return value != null ? value : defaultValue;
    }
}
