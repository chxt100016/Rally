package com.rally.db.user.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.user.convert.ProfileChangeLogConvertMapper;
import com.rally.db.user.entity.ProfileChangeLogPO;
import com.rally.db.user.repository.ProfileChangeLogRepository;
import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.log.gateway.ProfileChangeLogGateway;
import com.rally.domain.log.model.ProfileChangeLogData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProfileChangeLogGatewayImpl implements ProfileChangeLogGateway {

    private final ProfileChangeLogRepository repository;
    private static final ProfileChangeLogConvertMapper CONVERTER = ProfileChangeLogConvertMapper.INSTANCE;

    @Override
    public ProfileChangeLogData save(ProfileChangeLogData data) {
        ProfileChangeLogPO po = CONVERTER.toPO(data);
        po.setBizId(IdWorker.getIdStr());
        repository.save(po);
        return CONVERTER.toData(po);
    }

    public List<ProfileChangeLogData> findByUserIdAndType(String userId, ChangeLogTypeEnum type) {
        return repository.findByUserIdAndType(userId, type.name().toLowerCase())
                .stream()
                .map(CONVERTER::toData)
                .toList();
    }

    @Override
    public Optional<ProfileChangeLogData> findLatestUnderReviewLog(String userId) {
        return repository.findLatestUnderReviewLog(userId).map(CONVERTER::toData);
    }
}
