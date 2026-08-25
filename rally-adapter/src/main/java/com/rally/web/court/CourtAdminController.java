package com.rally.web.court;

import com.rally.court.CourtAdminAppService;
import com.rally.domain.court.model.CourtCollectApiCmd;
import com.rally.domain.court.model.CourtCollectResultDTO;
import com.rally.domain.court.model.CourtCreateApiCmd;
import com.rally.domain.court.model.CourtDisableApiCmd;
import com.rally.domain.court.model.CourtIdDTO;
import com.rally.domain.court.model.CourtUpdateApiCmd;
import com.rally.domain.tour.model.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 球场管理（运营后台）接口：抓取收录 / 新增 / 编辑 / 停用
 */
@RestController
@RequestMapping("/court/admin")
public class CourtAdminController {

    @Resource
    private CourtAdminAppService courtAdminAppService;

    /**
     * 按城市抓取球场并写入球场库
     */
    @PostMapping("/collect")
    public Result<CourtCollectResultDTO> collect(@Valid @RequestBody CourtCollectApiCmd cmd) {
        return Result.ok(courtAdminAppService.collect(cmd));
    }

    /**
     * 新增球场
     */
    @PostMapping("/create")
    public Result<CourtIdDTO> create(@Valid @RequestBody CourtCreateApiCmd cmd) {
        return Result.ok(courtAdminAppService.create(cmd));
    }

    /**
     * 编辑球场
     */
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody CourtUpdateApiCmd cmd) {
        courtAdminAppService.update(cmd);
        return Result.ok();
    }

    /**
     * 停用球场
     */
    @PostMapping("/disable")
    public Result<Void> disable(@Valid @RequestBody CourtDisableApiCmd cmd) {
        courtAdminAppService.disable(cmd);
        return Result.ok();
    }
}
