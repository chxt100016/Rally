package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.enums.GenderEnum;
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
     * 完成 onboarding 的用户基本信息部分（入参为 null 表示不更新）
     */
    public void completeOnboarding(String gender, LocalDate birthday, String cityCode) {
        if (gender != null) {
            updateGender(gender);
        }
        if (birthday != null) {
            this.birthday = birthday;
        }
        this.cityCode = cityCode;
    }

    /**
     * 解析并更新性别，非法值抛业务异常
     */
    public void updateGender(String gender) {
        try {
            this.gender = GenderEnum.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "性别值非法");
        }
    }

    /**
     * 校验基础信息（头像、昵称）已完善
     */
    public void assertBasicComplete() {
        Assert.isTrue(StringUtils.isNotBlank(avatarUrl) && StringUtils.isNotBlank(nickname), BizErrorCode.USER_INCOMPLETE);
    }
}
