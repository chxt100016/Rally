package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class PageDTO<T> {
    private List<T> list;
    private Long total;
    private Boolean hasMore;
    private String nextCursor;

    public PageDTO(List<T> list, Long total, Boolean hasMore) {
        this.list = list;
        this.total = total;
        this.hasMore = hasMore;
    }

    /** 将游标字段编码为 URL-safe Base64 JSON 对象，入参为 null/空返回 null */
    public static String encodeCursor(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.toJSONBytes(fields));
    }

    /** 解码游标；空串或非法游标按首页处理返回 null */
    public static Map<String, Object> decodeCursor(String encoded) {
        if (StringUtils.isBlank(encoded)) return null;
        try {
            return JSON.parseObject(Base64.getUrlDecoder().decode(encoded));
        } catch (Exception e) {
            return null;
        }
    }

    /** 将游标字段写入 nextCursor，hasMore 为 false 时不写 */
    public void buildCursor(Map<String, Object> fields) {
        if (!Boolean.TRUE.equals(hasMore)) return;
        this.nextCursor = encodeCursor(fields);
    }
}
