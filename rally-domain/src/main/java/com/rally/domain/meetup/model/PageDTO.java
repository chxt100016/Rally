package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.List;
import java.util.function.Function;

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

    /** 解析上一页游标为位置式值列表；空串/非法游标返回空列表（按首页处理） */
    public static List<Object> parseCursor(String encoded) {
        return decodeCursor(encoded);
    }

    /** 单游标字段（如 {@code buildCursor(MeetupCardDTO::getMeetupId)}），约定 bizId 固定在 index 0 */
    public void buildCursor(Function<? super T, Object> v1) {
        T last = lastForCursor();
        if (last != null) this.nextCursor = encodeCursor(List.of(v1.apply(last)));
    }

    /** 复合游标字段（如时间排序 {@code buildCursor(MeetupCardDTO::getMeetupId, c -> c.getStartTime().toString())}），bizId 固定在 index 0 */
    public void buildCursor(Function<? super T, Object> v1, Function<? super T, Object> v2) {
        T last = lastForCursor();
        if (last != null) this.nextCursor = encodeCursor(List.of(v1.apply(last), v2.apply(last)));
    }

    /** 可写游标时返回本页末条记录，否则返回 null（hasMore 非 true 或列表为空） */
    private T lastForCursor() {
        if (!Boolean.TRUE.equals(hasMore) || list == null || list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }

    /**
     * 内存游标分页：在已排序列表中定位 lastBizId，取其后 limit 条（不含 lastBizId 本身）。
     * 用于无法下推数据库的内存分页（如距离排序、内存筛选后分页）。
     * @param sorted    已排序的全量数据
     * @param lastBizId 上一页最后一条的 bizId（游标），null/空表示首页
     * @param limit     取条数（通常传 pageSize+1 便于调用方判断 hasMore）
     * @param bizIdOf   取记录 bizId 的方法
     */
    public static <E> List<E> sliceAfter(List<E> sorted, String lastBizId, int limit, Function<E, String> bizIdOf) {
        int start = 0;
        if (StringUtils.isNotBlank(lastBizId)) {
            for (int i = 0; i < sorted.size(); i++) {
                if (lastBizId.equals(bizIdOf.apply(sorted.get(i)))) {
                    start = i + 1;
                    break;
                }
            }
        }
        if (start >= sorted.size()) return List.of();
        return sorted.subList(start, Math.min(start + limit, sorted.size()));
    }

    /** 将游标值列表编码为 URL-safe Base64 JSON 数组，入参为 null/空返回 null */
    private static String encodeCursor(List<Object> values) {
        if (values == null || values.isEmpty()) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.toJSONBytes(values));
    }

    /** 解码游标；空串或非法游标按首页处理返回空列表 */
    private static List<Object> decodeCursor(String encoded) {
        if (StringUtils.isBlank(encoded)) return List.of();
        try {
            return JSON.parseArray(Base64.getUrlDecoder().decode(encoded));
        } catch (Exception e) {
            return List.of();
        }
    }
}
