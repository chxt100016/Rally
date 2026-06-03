package com.rally.db.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.user.entity.ProfileChangeLogPO;
import com.rally.db.user.mapper.ProfileChangeLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfileChangeLogService extends ServiceImpl<ProfileChangeLogMapper, ProfileChangeLogPO> {

    public ProfileChangeLogPO insert(ProfileChangeLogPO log) {
        this.save(log);
        return log;
    }

    public List<ProfileChangeLogPO> findByUserIdAndType(String userId, String type) {
        return this.lambdaQuery()
                .eq(ProfileChangeLogPO::getUserId, userId)
                .eq(ProfileChangeLogPO::getType, type)
                .orderByDesc(ProfileChangeLogPO::getCreateTime)
                .list();
    }

    public Optional<ProfileChangeLogPO> findLatestUnderReviewLog(String userId) {
        return Optional.ofNullable(
                this.lambdaQuery()
                        .eq(ProfileChangeLogPO::getUserId, userId)
                        .eq(ProfileChangeLogPO::getType, "under_review")
                        .orderByDesc(ProfileChangeLogPO::getCreateTime)
                        .last("LIMIT 1")
                        .one()
        );
    }
}
