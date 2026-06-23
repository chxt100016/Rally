package com.rally.web.system;

import com.rally.domain.tour.model.Result;
import com.rally.system.CityAppService;
import com.rally.system.model.CityDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 城市查询接口
 */
@RestController
@RequestMapping("/city")
@RequiredArgsConstructor
public class CityController {

    private final CityAppService cityAppService;

    /**
     * 查询所有城市
     */
    @GetMapping
    public Result<List<CityDTO>> list() {
        return Result.ok(cityAppService.listAll());
    }

    /**
     * 查询已开通城市
     */
    @GetMapping("/available")
    public Result<List<CityDTO>> available() {
        return Result.ok(cityAppService.listAvailable());
    }
}
