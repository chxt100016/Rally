package com.rally.domain.recap.model;

import com.rally.domain.recap.UserReviewDomainService;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MyReviewDTO {

    private Long total;
    private Long levelVoteCount;
    private Long attendanceVoteCount;
    private Long tagCount;
    private List<UserReviewDomainService.TagItem> tags;
}
