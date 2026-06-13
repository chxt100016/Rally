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
     * 校验基础信息（头像、昵称）已完善
     */
    public void assertBasicComplete() {
        Assert.isTrue(StringUtils.isNotBlank(avatarUrl) && StringUtils.isNotBlank(nickname), BizErrorCode.USER_INCOMPLETE);
    }
}
