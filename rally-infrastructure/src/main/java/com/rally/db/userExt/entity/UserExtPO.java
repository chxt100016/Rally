package com.rally.db.userExt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_ext")
public class UserExtPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String userId;
    private String extKey;
    private String extValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
