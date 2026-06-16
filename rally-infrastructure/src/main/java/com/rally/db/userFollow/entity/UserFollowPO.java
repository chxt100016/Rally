package com.rally.db.userFollow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_follow")
public class UserFollowPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 雪花 ID（业务主键） */
    private String bizId;
    /** 关注人 user_id */
    private String followerId;
    /** 被关注人 user_id */
    private String followingId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
