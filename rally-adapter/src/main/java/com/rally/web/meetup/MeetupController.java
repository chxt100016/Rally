package com.rally.web.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.tennis.model.Result;
import com.rally.meetup.MeetupAppService;
import com.rally.meetup.MeetupQueryService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 约球接口：发布/编辑/关闭/列表/详情
 */
@RestController
@RequestMapping("/wechat/meetup")
public class MeetupController {

    @Resource
    private MeetupAppService meetupAppService;

    @Resource
    private MeetupQueryService meetupQueryService;

    /**
     * 发布约球
     */
    @PostMapping("/publish")
    public Result<MeetupVO> publish(@Valid @RequestBody PublishCmd cmd) {
        return Result.ok(meetupAppService.publish(cmd));
    }

    /**
     * 编辑约球
     */
    @PostMapping("/edit")
    public Result<MeetupVO> edit(@Valid @RequestBody PublishCmd cmd) {
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
    public Result<PageVO<MeetupCardVO>> list(@Valid @RequestBody MeetupListQuery query) {
        return Result.ok(meetupQueryService.list(query));
    }

    /**
     * 约球详情
     */
    @GetMapping("/detail")
    public Result<MeetupVO> detail(@RequestParam("meetupId") String meetupId) {
        return Result.ok(meetupQueryService.detail(meetupId));
    }
}
