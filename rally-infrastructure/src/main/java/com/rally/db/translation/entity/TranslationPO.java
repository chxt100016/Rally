package com.rally.db.translation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("translation")
public class TranslationPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String entityType;
    private String originalText;
    private String language;
    private String translatedText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
