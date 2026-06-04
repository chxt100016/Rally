package com.rally.domain.user.model;

import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserData {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private GenderEnum gender;
    private LocalDate birthday;
    /** 个人简介 */
    private String bio;
    /** 用户当前城市编码 */
    private String cityCode;
}
