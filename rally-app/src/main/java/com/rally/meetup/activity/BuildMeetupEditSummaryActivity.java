package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupVO;
import com.rally.meetup.MeetupCardPackingService;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 build-meetup-edit-summary：由保存后的约球内存资料构建编辑摘要。
 */
@Component
@RequiredArgsConstructor
public class BuildMeetupEditSummaryActivity {

    private final MeetupCardPackingService meetupCardPackingService;

    public MeetupVO execute(MeetupData savedMeetup) {
        // A1：直接映射上游保存后的同一内存对象，保持现有编辑响应字段范围。
        MeetupVO summary = MeetupAppConvertMapper.INSTANCE.toMeetupVO(savedMeetup);

        // A2：复用统一卡片背景解析器，按最终球场资料和开始时段形成背景。
        summary.setBackgroundImage(meetupCardPackingService.resolveBackgroundKey(savedMeetup));

        // A3：返回不包含参与者、操作状态、复盘或人时分摊详情的编辑摘要。
        return summary;
    }
}
