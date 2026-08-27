package com.rally.domain.profilechangelog.model;

/** 建立档案变更日志时使用的雪花业务编号生成端口。 */
@FunctionalInterface
public interface ProfileChangeLogIdGenerator {

    String nextLogBizId();
}
