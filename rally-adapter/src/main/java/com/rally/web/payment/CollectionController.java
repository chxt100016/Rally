package com.rally.web.payment;

import com.rally.domain.payment.model.CollectionBatchDTO;
import com.rally.domain.payment.model.CollectionInitiateCmd;
import com.rally.domain.tour.model.Result;
import com.rally.payment.CollectionAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 收款接口：发起收款 / 关闭收款（活动结束后发起人触发）。
 */
@RestController
@RequestMapping("/payment/collection")
public class CollectionController {

    @Resource
    private CollectionAppService collectionAppService;

    /**
     * 发起收款
     */
    @PostMapping("/initiate")
    public Result<CollectionBatchDTO> initiate(@Valid @RequestBody CollectionInitiateCmd cmd) {
        return Result.ok(collectionAppService.initiate(cmd));
    }

    /**
     * 关闭收款
     */
    @PostMapping("/close")
    public Result<Void> close(@RequestParam("meetupId") String meetupId) {
        collectionAppService.close(meetupId);
        return Result.ok();
    }
}
