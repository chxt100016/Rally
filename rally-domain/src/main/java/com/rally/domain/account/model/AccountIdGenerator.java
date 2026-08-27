package com.rally.domain.account.model;

/** 建立账户时使用的业务编号生成器。 */
@FunctionalInterface
public interface AccountIdGenerator {

    String nextAccountId();
}
