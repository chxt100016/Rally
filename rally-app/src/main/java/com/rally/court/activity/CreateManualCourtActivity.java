package com.rally.court.activity;

import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.Court;
import com.rally.domain.court.model.CourtCreateCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 create-manual-court：写入一条手工录入球场，返回新生成的球场业务编号。
 */
@Component
@RequiredArgsConstructor
public class CreateManualCourtActivity {

    private final CourtRepository courtRepository;

    public String execute(CourtCreateCmd cmd) {
        // A1 以运营填写的资料与已解析的归属信息创建球场，
        // 业务编号、拼音、别名与标签的连接都由球场自身完成；不做重名或就近查重
        Court court = Court.create(cmd);
        // A2 单条写入，自成一个事务，失败时不产生任何记录
        courtRepository.save(court.state());
        // A3 返回新球场的业务编号
        return court.bizId();
    }
}
