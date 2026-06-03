package com.rally.domain.config.gateway;

/**
 * 全局配置读取网关。
 * 所有分值/阈值/文案从 sys_config 读取，缺失则回退 defaultValue。
 * 实现见 infrastructure 的 MysqlConfigGatewayImpl（启动加载 + 内存缓存）。
 */
public interface ConfigGateway {

    /**
     * 读字符串；key 不存在或未启用返回 defaultValue
     */
    String getString(String key, String defaultValue);

    /**
     * 读整型；解析失败回退 defaultValue
     */
    int getInt(String key, int defaultValue);

    /**
     * 读浮点；解析失败回退 defaultValue
     */
    float getFloat(String key, float defaultValue);

    /**
     * 读布尔；解析失败回退 defaultValue
     */
    boolean getBool(String key, boolean defaultValue);

    /**
     * 读 JSON 反序列化为 cls；解析失败回退 defaultValue。FastJson2 解析
     */
    <T> T getJson(String key, Class<T> cls, T defaultValue);

    /**
     * 带 scope 读字符串：先查 scope 专属值，未命中回退 global，再回退 defaultValue
     */
    String getString(String key, String scope, String defaultValue);
}
