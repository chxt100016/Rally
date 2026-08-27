package com.rally.domain.system.platformconfig;

/**
 * 随应用发布的配置名录定义。
 *
 * <p>实现负责标量或 JSON 结构校验，并把原始输入转换成紧凑、确定性的字符串。
 * 它不属于持久化聚合，也不得在这些方法中写数据库。</p>
 */
public interface PlatformConfigDefinition {

    String configKey();

    String valueType();

    String normalize(String rawValue);

    boolean accepts(String normalizedValue);
}
