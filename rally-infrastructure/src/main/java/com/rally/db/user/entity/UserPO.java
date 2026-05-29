package com.rally.db.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class UserPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
