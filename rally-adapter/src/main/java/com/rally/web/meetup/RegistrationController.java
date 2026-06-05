package com.rally.web.meetup;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.model.RegistrationVO;
import com.rally.domain.tennis.model.Result;
import com.rally.meetup.RegistrationAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
     * POST /api/rally/wechat/meetup/waitlist/join
     */
    @PostMapping("/join")
    public Result<Void> join(@RequestParam("meetupId") String meetupId,
                             @RequestParam(value = "autoWithdrawAt", required = false) LocalDateTime autoWithdrawAt) {
        try {
            registrationAppService.join(meetupId, autoWithdrawAt);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 撤回（仅 pending 可撤）
     * POST /api/rally/wechat/meetup/waitlist/withdraw
     */
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@RequestParam("meetupId") String meetupId) {
        try {
            registrationAppService.withdraw(meetupId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 退出（已加入）
     * POST /api/rally/wechat/meetup/waitlist/quit
     */
    @PostMapping("/quit")
    public Result<Void> quit(@RequestParam("meetupId") String meetupId) {
        try {
            registrationAppService.quit(meetupId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 审批通过
     * POST /api/rally/wechat/meetup/waitlist/approve
     */
    @PostMapping("/approve")
    public Result<Void> approve(@RequestParam("registrationId") String registrationId) {
        try {
            registrationAppService.approve(registrationId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 审批拒绝（仅创建人）
     * POST /api/rally/wechat/meetup/waitlist/reject
     */
    @PostMapping("/reject")
    public Result<Void> reject(@RequestParam("registrationId") String registrationId) {
        try {
            registrationAppService.reject(registrationId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 审批列表（仅创建人）
     * GET /api/rally/wechat/meetup/waitlist/pending
     */
    @GetMapping("/pending")
    public Result<List<RegistrationVO>> pendingList(@RequestParam("meetupId") String meetupId) {
        try {
            return Result.ok(registrationAppService.pendingList(meetupId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
