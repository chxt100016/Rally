package com.rally.domain.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {
    private String token;
    private String userId;
    private boolean isNewUser;
    private boolean needCompleteInfo;
}
