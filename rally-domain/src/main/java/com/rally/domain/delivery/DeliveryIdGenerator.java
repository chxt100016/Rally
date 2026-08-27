package com.rally.domain.delivery;

/** 为触达日志生成表内唯一的业务流水号。 */
@FunctionalInterface
public interface DeliveryIdGenerator {

    String nextDeliveryId();
}
