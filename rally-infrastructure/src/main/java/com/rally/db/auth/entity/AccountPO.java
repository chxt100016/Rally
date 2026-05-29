package com.rally.db.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("accounts")
public class AccountPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountId;
    private String userId;
    private String channel;
    private String identifier;
    private String credential;
    private String unionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
