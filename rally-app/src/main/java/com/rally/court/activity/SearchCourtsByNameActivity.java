package com.rally.court.activity;

import com.rally.domain.court.convert.CourtConvertMapper;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtSearchCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 search-courts-by-name：按城市和名称或别名搜索可用球场。
 */
@Component
@RequiredArgsConstructor
public class SearchCourtsByNameActivity {

    private final CourtRepository courtRepository;

    public List<CourtDTO> execute(CourtSearchCmd cmd) {
        // A1 仓储按城市、ACTIVE 状态以及可选的名称/别名条件查询；空白词不追加名称条件
        // A2 拆分别名和标签，并按既有转换规则生成球场环境展示名
        // A3 解析扩展资料，组装对外球场列表
        return CourtConvertMapper.INSTANCE.toDTOList(
                courtRepository.fuzzySearchByName(cmd.getCityCode(), cmd.getQuery()));
    }
}
