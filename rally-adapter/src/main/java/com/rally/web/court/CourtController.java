package com.rally.web.court;

import com.rally.court.CourtQueryAppService;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtQueryCmd;
import com.rally.domain.tour.model.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 球场接口：查询
 */
@RestController
@RequestMapping("/court")
public class CourtController {

    @Resource
    private CourtQueryAppService courtQueryAppService;

    /**
     * 模糊搜索球场
     */
    @GetMapping("/search")
    public Result<List<CourtDTO>> search(@RequestParam("cityCode") String cityCode,
                                          @RequestParam(value = "keyword", required = false) String keyword) {
        CourtQueryCmd cmd = new CourtQueryCmd();
        cmd.setCityCode(cityCode);
        cmd.setKeyword(keyword);
        return Result.ok(courtQueryAppService.search(cmd));
    }
}
