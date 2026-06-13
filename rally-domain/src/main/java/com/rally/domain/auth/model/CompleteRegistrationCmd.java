package com.rally.domain.auth.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.rally.domain.user.enums.GenderEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompleteRegistrationCmd {
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "头像不能为空")
    private String avatarUrl;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime birthday;

    private GenderEnum gender;

    private String cityCode;
}
