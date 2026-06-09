package com.rally.domain.log.model;

import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.user.enums.ChangeReasonEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 变更日志领域数据
 */
@Data
public class ProfileChangeLogData {
    private String bizId;
    private String userId;
    private ChangeLogTypeEnum type;
    private BigDecimal beforeValue;
    private BigDecimal afterValue;
    private BigDecimal value;
    private ChangeReasonEnum reason;
    private String remark;
    private String refId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
