package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MyProfileStatsDTO {

    private Long followerCount;

    private Long followingCount;

    private Long completedCount;
}
