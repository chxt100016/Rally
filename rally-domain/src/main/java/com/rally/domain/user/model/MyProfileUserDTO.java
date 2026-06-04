package com.rally.domain.user.model;

import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 我的档案 - 用户信息
 */
@Data
@Accessors(chain = true)
public class MyProfileUserDTO {

    private String userId;
    private String nickname;
    private GenderEnum gender;
    private LocalDate birthday;
    /** 用户当前城市编码 */
    private String cityCode;
    /** 个人简介 */
    private String bio;
    /** 头像URL */
    private String avatarUrl;
}
