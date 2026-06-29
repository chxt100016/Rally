package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.enums.UserConst;
import com.rally.domain.utils.Assert;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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



    /**
     * 基础信息是否为默认值（头像、昵称）
     */
    public boolean isBasicDefault() {
        return UserConst.DEFAULT_AVATAR_URL.equals(avatarUrl) || UserConst.DEFAULT_NICKNAME.equals(nickname);
    }
}
