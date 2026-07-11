package com.rally.db.userExt.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.userExt.convert.UserExtConvertMapper;
import com.rally.db.userExt.entity.UserExtPO;
import com.rally.db.userExt.service.UserExtService;
import com.rally.domain.user.gateway.UserExtRepository;
import com.rally.domain.user.model.UserExtData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserExtRepositoryImpl implements UserExtRepository {

    private final UserExtService userExtService;
    private static final UserExtConvertMapper MAPPER = UserExtConvertMapper.INSTANCE;

    @Override
    public void save(UserExtData data) {
        UserExtPO po = MAPPER.toPO(data);
        if (data.getBizId() == null) {
            po.setBizId(IdWorker.getIdStr());
        }
        UserExtPO existing = userExtService.lambdaQuery().eq(UserExtPO::getUserId, data.getUserId()).eq(UserExtPO::getExtKey, data.getExtKey()).one();
        if (existing != null) {
            po.setId(existing.getId());
            userExtService.updateById(po);
        } else {
            userExtService.save(po);
        }
    }

    @Override
    public Optional<UserExtData> findByUserIdAndKey(String userId, String extKey) {
        UserExtPO po = userExtService.lambdaQuery().eq(UserExtPO::getUserId, userId).eq(UserExtPO::getExtKey, extKey).one();
        return Optional.ofNullable(po).map(MAPPER::toData);
    }

    @Override
    public List<UserExtData> findAllByUserId(String userId) {
        List<UserExtPO> poList = userExtService.lambdaQuery().eq(UserExtPO::getUserId, userId).list();
        return MAPPER.toDataList(poList);
    }

    @Override
    public void deleteByUserIdAndKey(String userId, String extKey) {
        userExtService.lambdaUpdate().eq(UserExtPO::getUserId, userId).eq(UserExtPO::getExtKey, extKey).remove();
    }
}
