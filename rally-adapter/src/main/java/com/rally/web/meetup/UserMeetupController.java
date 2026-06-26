package com.rally.web.meetup;

import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.tour.model.Result;
import com.rally.meetup.UserMeetupAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户约球列表接口（待处理/进行中/我发布/已完成）
 */
@RestController
@RequestMapping("/wechat/meetup/user")
public class UserMeetupController {

    @Resource
    private UserMeetupAppService userMeetupAppService;

    /**
     * 用户约球列表（按 Tab 筛选）
     */
    @PostMapping("/list")
    public Result<PageDTO<MeetupCardDTO>> list(@Valid @RequestBody UserMeetupListCmd cmd) {
        return Result.ok(userMeetupAppService.queryUserMeetupList(cmd));
    }
}
