package com.rally.web.meetup;

import com.rally.domain.meetup.model.MeetupJoinCmd;
import com.rally.domain.meetup.model.MeetupQuitCmd;
import com.rally.domain.meetup.model.MeetupWithdrawCmd;
import com.rally.domain.meetup.model.RegistrationApproveCmd;
import com.rally.domain.meetup.model.RegistrationRejectCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.meetup.RegistrationAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报名/注册接口：报名/撤回/退出/审批
 * 对外接口仍使用 waitlist 路径
 */
@RestController
@RequestMapping("/wechat/meetup/registration")
public class RegistrationController {

    @Resource
    private RegistrationAppService registrationAppService;

    /**
     * 报名
     * POST /api/rally/wechat/meetup/registration/join
     */
    @PostMapping("/join")
    public Result<Void> join(@RequestBody @Valid MeetupJoinCmd cmd) {
        registrationAppService.join(cmd.getMeetupId(), cmd.getAutoWithdrawAt());
        return Result.ok();
    }

    /**
     * 撤回（仅 pending 可撤）
     * POST /api/rally/wechat/meetup/registration/withdraw
     */
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@RequestBody @Valid MeetupWithdrawCmd cmd) {
        registrationAppService.withdraw(cmd.getMeetupId());
        return Result.ok();
    }

    /**
     * 退出（已加入）
     * POST /api/rally/wechat/meetup/registration/quit
     */
    @PostMapping("/quit")
    public Result<Void> quit(@RequestBody @Valid MeetupQuitCmd cmd) {
        registrationAppService.quit(cmd.getMeetupId());
        return Result.ok();
    }

    /**
     * 审批通过
     * POST /api/rally/wechat/meetup/registration/approve
     */
    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody @Valid RegistrationApproveCmd cmd) {
        registrationAppService.approve(cmd.getRegistrationId());
        return Result.ok();
    }

    /**
     * 审批拒绝（仅创建人）
     * POST /api/rally/wechat/meetup/registration/reject
     */
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody @Valid RegistrationRejectCmd cmd) {
        registrationAppService.reject(cmd.getRegistrationId());
        return Result.ok();
    }
}
