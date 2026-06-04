package com.rally.web.meetup;

import com.rally.domain.auth.exception.BusinessException;
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
     * POST /api/rally/wechat/meetup/publish
     */
    @PostMapping("/publish")
    public Result<MeetupVO> publish(@Valid @RequestBody PublishCmd cmd) {
        try {
            return Result.ok(meetupAppService.publish(cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 编辑约球
     * POST /api/rally/wechat/meetup/edit
     */
    @PostMapping("/edit")
    public Result<MeetupVO> edit(@Valid @RequestBody PublishCmd cmd) {
        try {
            return Result.ok(meetupAppService.edit(cmd));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 关闭约球
     * POST /api/rally/wechat/meetup/close
     */
    @PostMapping("/close")
    public Result<Void> close(@RequestParam("meetupId") String meetupId) {
        try {
            meetupAppService.close(meetupId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 约球列表
     * GET /api/rally/wechat/meetup/list
     */
    @GetMapping("/list")
    public Result<PageVO<MeetupCardVO>> list(@Valid MeetupListQuery query) {
        try {
            return Result.ok(meetupQueryService.list(query));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 约球详情
     * GET /api/rally/wechat/meetup/detail
     */
    @GetMapping("/detail")
    public Result<MeetupVO> detail(@RequestParam("meetupId") String meetupId) {
        try {
            return Result.ok(meetupQueryService.detail(meetupId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
