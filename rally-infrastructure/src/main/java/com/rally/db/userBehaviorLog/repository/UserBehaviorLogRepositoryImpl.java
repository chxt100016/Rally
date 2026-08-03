package com.rally.db.userBehaviorLog.repository;

import com.rally.db.userBehaviorLog.convert.UserBehaviorLogConvertMapper;
import com.rally.db.userBehaviorLog.service.UserBehaviorLogDbService;
import com.rally.domain.behavior.gateway.UserBehaviorLogRepository;
import com.rally.domain.behavior.model.UserBehaviorLogData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBehaviorLogRepositoryImpl implements UserBehaviorLogRepository {

    private static final UserBehaviorLogConvertMapper MAPPER = UserBehaviorLogConvertMapper.INSTANCE;

    private final UserBehaviorLogDbService userBehaviorLogDbService;

    @Override
    public void save(UserBehaviorLogData data) {
        userBehaviorLogDbService.save(MAPPER.toPO(data));
    }
}
