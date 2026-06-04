package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 我的档案 - 约球信息
 */
@Data
@Accessors(chain = true)
public class MyProfileMeetupDTO {

    /** 已完成约球次数 */
    private Integer completedCount;
}
