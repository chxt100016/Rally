package com.rally.domain.user.model;

import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserVO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private GenderEnum gender;
    private LocalDate birthday;
}
