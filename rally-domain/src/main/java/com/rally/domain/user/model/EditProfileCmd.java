package com.rally.domain.user.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * 编辑资料入参（不含 NTRP）
 */
@Data
public class EditProfileCmd {
    private String nickname;
    private String avatarUrl;
    private String gender;
    private LocalDate birthday;
    private String cityCode;
    private String bio;
}
