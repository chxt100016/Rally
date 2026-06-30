package com.rally.web.court;

import com.rally.court.CourtQueryAppService;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtListCmd;
import com.rally.domain.court.model.CourtSearchCmd;
import com.rally.domain.tour.model.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/court")
public class CourtController {

    @Resource
    private CourtQueryAppService courtQueryAppService;

    @PostMapping("/list")
    public Result<List<CourtDTO>> list(@Valid @RequestBody CourtListCmd cmd) {
        return Result.ok(courtQueryAppService.getAll(cmd));
    }

    @PostMapping("/search")
    public Result<List<CourtDTO>> search(@Valid @RequestBody CourtSearchCmd cmd) {
        if (cmd.getQuery() == null || cmd.getQuery().length() < 2) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(courtQueryAppService.search(cmd));
    }
}
