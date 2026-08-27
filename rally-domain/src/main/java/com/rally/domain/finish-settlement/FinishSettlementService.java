package com.rally.domain.meetup.finishsettlement;

import com.rally.domain.meetup.gateway.MeetupRepository;
import org.springframework.stereotype.Service;
import java.util.Objects;

/**
 * 使用单条数据库更新结算已到结束时间的约球。
 */
@Service
public class FinishSettlementService {

    private final MeetupRepository meetupRepository;

    public FinishSettlementService(MeetupRepository meetupRepository) {
        this.meetupRepository = Objects.requireNonNull(meetupRepository, "meetupRepository");
    }

    /**
     * 将存储状态精确为 {@code OPEN} 或 {@code full}，且结束时间早于
     * 构造批量更新语句时当前时间的约球置为 {@code FINISHED}。
     *
     * <p>筛选、状态迁移和影响行数统计均由仓储的同一条 UPDATE 完成；
     * 本服务不读取候选、不归一化状态、不重试，更新异常原样传播。</p>
     *
     * @return 本次由单条更新置为 {@code FINISHED} 的记录数，无命中时为 0
     */
    public Integer settle() {
        return meetupRepository.batchUpdateToFinished();
    }
}
