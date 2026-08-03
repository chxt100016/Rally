package com.rally.domain.behavior.gateway;

import com.rally.domain.behavior.model.UserBehaviorLogData;

public interface UserBehaviorLogRepository {

    void save(UserBehaviorLogData data);
}
