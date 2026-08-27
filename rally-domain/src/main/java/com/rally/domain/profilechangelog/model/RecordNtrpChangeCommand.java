package com.rally.domain.profilechangelog.model;

import java.math.BigDecimal;

/** C1 记录 NTRP 变化的命令入参；原因固定为 USER。 */
public record RecordNtrpChangeCommand(
        String userId,
        BigDecimal beforeValue,
        BigDecimal afterValue,
        String remark,
        String refId) {
}
