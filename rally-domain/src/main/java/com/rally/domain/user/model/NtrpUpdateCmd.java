package com.rally.domain.user.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 自评修改入参
 */
@Data
public class NtrpUpdateCmd {
    /** 目标自评 1.5~7.0 步长 0.5 */
    private BigDecimal ntrpScore;
    /** 前端确认后置 true */
    private Boolean confirmed;
}
