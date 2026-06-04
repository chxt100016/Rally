package com.rally.domain.user.model;

import lombok.Data;

/**
 * 我的信息聚合视图
 */
@Data
public class MyUserProfileDTO {

    /** 基础用户信息 */
    private UserVO user;
    /** 网球档案 */
    private TennisProfileVO profile;

}
