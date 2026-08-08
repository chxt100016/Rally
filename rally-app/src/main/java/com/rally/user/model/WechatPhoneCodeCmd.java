package com.rally.user.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatPhoneCodeCmd {

    @NotBlank(message = "微信手机号动态令牌不能为空")
    private String code;
}
