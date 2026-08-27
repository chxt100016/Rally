package com.rally.domain.identity.userextension;

/** 每次保存用户扩展资料时使用的雪花业务编号生成端口。 */
@FunctionalInterface
public interface UserExtensionIdGenerator {

    String nextBusinessId();
}
