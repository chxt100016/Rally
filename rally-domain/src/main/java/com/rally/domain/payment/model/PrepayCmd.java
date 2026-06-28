package com.rally.domain.payment.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取支付参数入参（参与人点支付时调）。
 */
@Data
public class PrepayCmd {

    /** 支付单 bizId（== 渠道 out_trade_no） */
    @NotBlank(message = "支付单号不能为空")
    private String paymentId;
}
