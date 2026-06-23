package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 列表 searchAfter 游标。
 * 对前端不透明：前端取上次响应的 nextCursor 原样回传，无需理解内部结构。
 * - 按时间排序：bizId + startTime 组成 (startTime, bizId) 复合游标
 * - 其它排序：仅 bizId
 */
@Data
public class PageCursor {
    /** 上一页最后一条的 bizId（唯一 tie-breaker） */
    private String bizId;
    /** 上一页最后一条的开球时间（按时间排序时填充） */
    private LocalDateTime startTime;

    /** 仅 bizId 的游标 */
    public static PageCursor ofBizId(String bizId) {
        PageCursor cursor = new PageCursor();
        cursor.setBizId(bizId);
        return cursor;
    }

    /** (startTime, bizId) 复合游标 */
    public static PageCursor ofTime(LocalDateTime startTime, String bizId) {
        PageCursor cursor = new PageCursor();
        cursor.setStartTime(startTime);
        cursor.setBizId(bizId);
        return cursor;
    }

    /** 编码为 URL-safe Base64 字符串，供前端原样回传；入参为 null 返回 null */
    public static String encode(PageCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.toJSONBytes(cursor));
    }

    /** 解码游标；空串或非法游标统一按首页处理返回 null */
    public static PageCursor decode(String cursor) {
        if (StringUtils.isBlank(cursor)) {
            return null;
        }
        try {
            return JSON.parseObject(Base64.getUrlDecoder().decode(cursor), PageCursor.class);
        } catch (Exception e) {
            return null;
        }
    }
}
