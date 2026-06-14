package com.rally.domain.system;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.gateway.SysConfigLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置静态提供者。
 * 启动时通过 SysConfigLoader 全量加载到内存。
 * 对外提供静态方法，调用方无需注入。
 */
@Slf4j
@Component
public class SystemConfig {

    private final SysConfigLoader loader;

    /** 静态单例引用 */
    private static volatile SystemConfig instance;

    /** 缓存键 = scope + '|' + configKey，value = 字符串化的 configValue */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /** key -> defaultValue 映射，从枚举初始化，用于缓存未命中时回退 */
    private static final Map<String, String> KEY_DEFAULT_MAP = new ConcurrentHashMap<>();

    static {
        // 从枚举构建 key -> defaultValue 映射
        Arrays.stream(SystemConfigKey.values()).forEach(e -> KEY_DEFAULT_MAP.put(e.getKey(), e.getDefaultValue()));
    }

    public SystemConfig(SysConfigLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    public void init() {
        instance = this;
        reload();
    }

    // ==================== 静态方法 ====================

    /**
     * 获取字符串配置值。
     * 若缓存中不存在该 key，则从枚举 SystemConfigKey 的 defaultValue 中获取；
     * 若枚举中也不存在，则返回传入的 defaultValue 参数。
     */
    public static String getString(String key, String defaultValue) {
        return getString(key, "global", defaultValue);
    }

    /**
     * 获取 int 配置值。
     * 若缓存中不存在该 key，则从枚举 SystemConfigKey 的 defaultValue 中获取；
     * 若枚举中也不存在，则返回传入的 defaultValue 参数。
     */
    public static int getInt(String key, int defaultValue) {
        // 先尝试从枚举默认值获取
        String enumDefault = KEY_DEFAULT_MAP.get(key);
        String value = getString(key, "global", enumDefault != null ? enumDefault : String.valueOf(defaultValue));
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

    /**
     * 获取 float 配置值。
     * 若缓存中不存在该 key，则从枚举 SystemConfigKey 的 defaultValue 中获取；
     * 若枚举中也不存在，则返回传入的 defaultValue 参数。
     */
    public static float getFloat(String key, float defaultValue) {
        String enumDefault = KEY_DEFAULT_MAP.get(key);
        String value = getString(key, "global", enumDefault != null ? enumDefault : String.valueOf(defaultValue));
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

    /**
     * 获取 boolean 配置值。
     * 若缓存中不存在该 key，则从枚举 SystemConfigKey 的 defaultValue 中获取；
     * 若枚举中也不存在，则返回传入的 defaultValue 参数。
     */
    public static boolean getBool(String key, boolean defaultValue) {
        String enumDefault = KEY_DEFAULT_MAP.get(key);
        String value = getString(key, "global", enumDefault != null ? enumDefault : String.valueOf(defaultValue));
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

    /**
     * 获取 JSON 配置值。
     * 若缓存中不存在该 key，则从枚举 SystemConfigKey 的 defaultValue 中获取；
     * 若枚举中也不存在，则返回传入的 defaultValue 参数。
     */
    public static <T> T getJson(String key, Class<T> cls, T defaultValue) {
        String enumDefault = KEY_DEFAULT_MAP.get(key);
        String value = getString(key, "global", enumDefault);
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

    /**
     * 获取字符串配置值（带 scope）。
     * 优先级：scope 专属值 > global 值 > 枚举默认值 > 传入的 defaultValue
     */
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

        // 缓存未命中时，尝试从枚举默认值获取
        if (value == null) {
            value = KEY_DEFAULT_MAP.get(key);
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
