package com.rally.court.activity;

import com.rally.domain.court.convert.CourtConvertMapper;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtListCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 list-city-courts：查询指定城市的可用球场并整理对外字段。
 */
@Component
@RequiredArgsConstructor
public class ListCityCourtsActivity {

    private final CourtRepository courtRepository;

    public List<CourtDTO> execute(CourtListCmd cmd) {
        // A1 按城市编码精确查询 ACTIVE 球场，状态过滤由仓储的既有查询保证
        // A2 按既有转换规则拆分别名、标签并生成球场环境展示名
        // A3 解析扩展资料并组装对外清单；既有转换不把 meetup_count 写入 total
        return CourtConvertMapper.INSTANCE.toDTOList(courtRepository.findByCityCode(cmd.getCityCode()));
    }
}
