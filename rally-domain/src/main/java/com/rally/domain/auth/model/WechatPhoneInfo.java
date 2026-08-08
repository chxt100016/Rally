package com.rally.domain.auth.model;

import lombok.Data;

@Data
public class WechatPhoneInfo {
    private String phoneNumber;
    private String purePhoneNumber;
    private String countryCode;
}
