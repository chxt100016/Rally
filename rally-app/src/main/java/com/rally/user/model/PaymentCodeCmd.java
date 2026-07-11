package com.rally.user.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentCodeCmd {
    @NotBlank(message = "付款码 key 不能为空")
    private String key;
}
