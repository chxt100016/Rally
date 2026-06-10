package com.rally.domain.meetup.enums;

/**
 * 用户约球列表 Tab 枚举
 */
public enum UserMeetupTabEnum {
    /** 待处理：创建人待审批 + 参与者未评价/未录比分 */
    PENDING,
    /** 进行中：status=OPEN/FULL 且未到结束时间 */
    IN_PROGRESS,
    /** 我发布：创建人是当前用户 */
    MY_PUBLISH,
    /** 已完成：status=FINISHED/CLOSED 或懒判定已结束 */
    COMPLETED,
    /** 最近：用户最近完成的约球（球员主页用） */
    RECENT
}
