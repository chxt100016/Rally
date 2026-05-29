package com.rally.domain.auth.model;

import lombok.Data;

@Data
public class WechatLoginCmd {
    private String code;
}
