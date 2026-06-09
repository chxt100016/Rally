package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户约球列表查询入参
 */
@Data
public class UserMeetupListCmd {
    /** 筛选 Tab */
    @NotNull(message = "请选择tab")
    private UserMeetupTabEnum tab;
    /** 页码，默认1 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNo = 1;
    /** 每页数量，默认10 */
    @Min(value = 1, message = "每页数量最小为1")
    private Integer pageSize = 10;
}
