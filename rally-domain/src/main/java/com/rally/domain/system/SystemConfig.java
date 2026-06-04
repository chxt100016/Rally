package com.rally.domain.system;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.system.event.ConfigRefreshEvent;
import com.rally.domain.system.gateway.SysConfigLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置静态提供者。
 * 启动时通过 SysConfigLoader 全量加载到内存，支持 ConfigRefreshEvent 刷新。
 * 对外提供静态方法，调用方无需注入。
 */
@Slf4j
@Component
public class SystemConfig implements ApplicationListener<ConfigRefreshEvent> {

    private final SysConfigLoader loader;

    /** 静态单例引用 */
    private static volatile SystemConfig instance;

    /** 缓存键 = scope + '|' + configKey，value = 字符串化的 configValue */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public SystemConfig(SysConfigLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    public void init() {
        instance = this;
        reload();
    }

    @Override
    public void onApplicationEvent(ConfigRefreshEvent event) {
        reload();
    }

    // ==================== 静态方法 ====================

    public static String getString(String key, String defaultValue) {
        return getString(key, "global", defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
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

    public static float getFloat(String key, float defaultValue) {
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

    public static boolean getBool(String key, boolean defaultValue) {
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

    public static <T> T getJson(String key, Class<T> cls, T defaultValue) {
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

    public static String getString(String key, String scope, String defaultValue) {
        SystemConfig inst = instance;
        if (inst == null) {
            log.warn("SystemConfig 尚未初始化，key={} 回退默认值", key);
            return defaultValue;
        }
        // 先查 scope 专属值
        String cacheKey = scope + "|" + key;
        String value = inst.cache.get(cacheKey);

        // scope 非 global 时，未命中回退 global
        if (value == null && !"global".equals(scope)) {
            cacheKey = "global|" + key;
            value = inst.cache.get(cacheKey);
        }

        return value != null ? value : defaultValue;
    }

    // ==================== 内部方法 ====================

    private void reload() {
        try {
            Map<String, String> allConfigs = loader.loadAll();
            cache.clear();
            cache.putAll(allConfigs);
            log.info("配置缓存加载完成，共 {} 项", cache.size());
        } catch (Exception e) {
            log.error("配置缓存加载失败", e);
        }
    }
}
