package com.rally.web.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.tennis.model.Result;
import com.rally.meetup.MeetupAppService;
import com.rally.meetup.MeetupDetailAppService;
import com.rally.meetup.MeetupQueryAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.web.bind.annotation.*;

/**
 * 约球接口：发布/编辑/关闭/列表/详情
 */
@RestController
@RequestMapping("/meetup")
public class MeetupController {


    @Resource
    private MeetupAppService meetupAppService;
    @Resource
    private MeetupQueryAppService meetupQueryAppService;
    @Resource
    private MeetupDetailAppService meetupDetailAppService;

    /**
     * 发布约球
     */
    @PostMapping("/publish")
    public Result<?> publish(@Valid @RequestBody MeetupPublishCmd cmd) {
        meetupAppService.publish(cmd);
        return Result.ok();
    }

    /**
     * 编辑约球
     */
    @PostMapping("/edit")
    public Result<MeetupVO> edit(@Valid @RequestBody MeetupEditCmd cmd) {
        return Result.ok(meetupAppService.edit(cmd));
    }

    /**
     * 关闭约球
     */
    @PostMapping("/close")
    public Result<Void> close(@RequestParam("meetupId") String meetupId) {
        meetupAppService.close(meetupId);
        return Result.ok();
    }

    /**
     * 约球列表
     */
    @PostMapping("/list")
    public Result<PageDTO<MeetupCardDTO>> list(@Valid @RequestBody MeetupListCmd query) {
        return Result.ok(meetupQueryAppService.queryMeetupList(query));
    }

    /**
     * 约球详情
     */
    @GetMapping("/detail/{meetupId}")
    public Result<MeetupDetailDTO> detail(@PathVariable("meetupId") String meetupId) {
        return Result.ok(meetupDetailAppService.detail(meetupId));
    }
}
