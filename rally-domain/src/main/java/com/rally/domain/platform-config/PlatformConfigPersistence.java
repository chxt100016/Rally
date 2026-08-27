package com.rally.domain.system.platformconfig;

/**
 * {@code sys_config} 的唯一领域写端口。
 *
 * <p>更新实现必须使用 {@code WHERE id=? AND version=?}，在同一 SQL 中写业务字段并执行
 * {@code version=version+1}；返回 false 表示版本竞争失败。</p>
 */
public interface PlatformConfigPersistence {

    PlatformConfigInsertResult insert(PlatformConfigState state);

    boolean publishIfVersion(
            long id,
            int expectedVersion,
            String normalizedValue,
            String description);

    boolean disableIfVersion(long id, int expectedVersion);
}
