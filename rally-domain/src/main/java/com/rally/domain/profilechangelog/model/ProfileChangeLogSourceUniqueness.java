package com.rally.domain.profilechangelog.model;

/** 非空来源事件的日志幂等预检端口。 */
@FunctionalInterface
public interface ProfileChangeLogSourceUniqueness {

    boolean exists(ProfileChangeLogSourceKey sourceKey);
}
