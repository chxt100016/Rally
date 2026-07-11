package com.rally.user.model;

import lombok.Data;

@Data
public class PaymentCodeDTO {
    private String key;
    private String paymentCodeUrl;
}
