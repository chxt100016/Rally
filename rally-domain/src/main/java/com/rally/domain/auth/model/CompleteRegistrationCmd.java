package com.rally.domain.auth.model;

import lombok.Data;

@Data
public class CompleteRegistrationCmd {
    private String nickname;
    private String avatarUrl;
    private String birthday;
    private String gender;
}
