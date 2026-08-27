package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.MeetupSortEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.meetup.MeetupCardPackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 pack-meetup-square-cards：截取候选窗口并组装约球广场卡片页。
 */
@Component
@RequiredArgsConstructor
public class PackMeetupSquareCardsActivity {

    private final MeetupCardPackingService packingService;

    public PageDTO<MeetupCardDTO> execute(List<MeetupData> candidates,
                                          Integer pageSize,
                                          MeetupSortEnum sort,
                                          Double lng,
                                          Double lat) {
        // A1：候选窗口按 pageSize 多取一项，只将当前页数据交给卡片映射。
        boolean hasMore = candidates.size() > pageSize;
        List<MeetupData> pageData = hasMore ? candidates.subList(0, pageSize) : candidates;

        // A2-A3：映射基础字段、区域主标签、查询点距离和球场背景降级结果。
        List<MeetupCardDTO> cards = pageData.stream()
                .map(candidate -> packingService.packCard(candidate, lng, lat))
                .toList();

        // A4：总量固定为空；仅后续仍有数据时，由当前页末项生成排序对应的游标。
        PageDTO<MeetupCardDTO> page = new PageDTO<>(cards, null, hasMore);
        if (sort == MeetupSortEnum.TIME) {
            page.buildCursor(MeetupCardDTO::getMeetupId, card -> card.getStartTime().toString());
        } else {
            page.buildCursor(MeetupCardDTO::getMeetupId);
        }
        return page;
    }
}
