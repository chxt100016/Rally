package com.rally.db.user.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.ProfileChangeLogConvertMapper;
import com.rally.db.user.entity.ProfileChangeLogPO;
import com.rally.db.user.service.ProfileChangeLogService;
import com.rally.domain.log.gateway.ProfileChangeLogRepository;
import com.rally.domain.log.model.ProfileChangeLogData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProfileChangeLogRepositoryImpl implements ProfileChangeLogRepository {

    private final ProfileChangeLogService profileChangeLogService;
    private static final ProfileChangeLogConvertMapper CONVERTER = ProfileChangeLogConvertMapper.INSTANCE;

    @Override
    public ProfileChangeLogData save(ProfileChangeLogData data) {
        ProfileChangeLogPO po = CONVERTER.toPO(data);
        po.setBizId(IdWorker.getIdStr());
        profileChangeLogService.save(po);
        return CONVERTER.toData(po);
    }

    @Override
    public Optional<ProfileChangeLogData> findLatestUnderReviewLog(String userId) {
        return profileChangeLogService.findLatestUnderReviewLog(userId).map(CONVERTER::toData);
    }
}
