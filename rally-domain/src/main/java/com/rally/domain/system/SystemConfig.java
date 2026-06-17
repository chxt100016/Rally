package com.rally.domain.system;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.gateway.SysConfigLoader;

/**
 * 系统配置读取工具类，提供 getString / getInt / getLong / getBigDecimal 等便捷方法。
 * 底层数据通过 {@link SysConfigLoader} 从 DB 加载并缓存在内存中。
 * 若 DB 中无对应配置，则使用 {@link SystemConfigKey} 枚举中定义的默认值。
 */
public class SystemConfig {

    private static final Map<String, String> systemConfigMap = new ConcurrentHashMap<>();
    private static SysConfigLoader sysConfigLoader;

    public static void setSysConfigLoader(SysConfigLoader sysConfigLoader) {
        SystemConfig.sysConfigLoader = sysConfigLoader;
    }

    public static void init() {
        systemConfigMap.clear();
        sysConfigLoader.loadAll().forEach(systemConfigMap::put);
    }

    // ==================== 便捷读取方法 ====================

    /**
     * 获取字符串类型的配置值，若 DB 中不存在则使用枚举默认值
     *
     * @param key 配置 key
     * @return 配置值、枚举默认值或 null
     */
    public static String getString(String key) {
        String val = systemConfigMap.get(key);
        if (val != null) {
            return val;
        }
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        return configKey != null ? configKey.getDefaultValue() : null;
    }

    /**
     * 获取整型配置值，若 DB 中不存在则使用枚举默认值
     *
     * @param key 配置 key
     * @return 配置值、枚举默认值或 0
     */
    public static int getInt(String key) {
        String val = systemConfigMap.get(key);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        if (configKey != null) {
            try {
                return Integer.parseInt(configKey.getDefaultValue());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 获取长整型配置值，若 DB 中不存在则使用枚举默认值
     *
     * @param key 配置 key
     * @return 配置值、枚举默认值或 0L
     */
    public static long getLong(String key) {
        String val = systemConfigMap.get(key);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        if (configKey != null) {
            try {
                return Long.parseLong(configKey.getDefaultValue());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 获取浮点型配置值，若 DB 中不存在则使用枚举默认值
     *
     * @param key 配置 key
     * @return 配置值、枚举默认值或 0f
     */
    public static float getFloat(String key) {
        String val = systemConfigMap.get(key);
        if (val != null) {
            try {
                return Float.parseFloat(val);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        if (configKey != null) {
            try {
                return Float.parseFloat(configKey.getDefaultValue());
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        return 0f;
    }

    /**
     * 获取 BigDecimal 类型的配置值，若 DB 中不存在则使用枚举默认值
     *
     * @param key 配置 key
     * @return 配置值、枚举默认值或 BigDecimal.ZERO
     */
    public static BigDecimal getBigDecimal(String key) {
        String val = systemConfigMap.get(key);
        if (val != null) {
            return new BigDecimal(val);
        }
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        if (configKey != null) {
            return new BigDecimal(configKey.getDefaultValue());
        }
        return BigDecimal.ZERO;
    }
}
