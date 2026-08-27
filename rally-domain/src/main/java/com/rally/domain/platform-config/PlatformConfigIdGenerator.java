package com.rally.domain.system.platformconfig;

/** 首次发布平台配置时生成雪花业务编号的端口。 */
@FunctionalInterface
public interface PlatformConfigIdGenerator {

    String nextBizId();
}
