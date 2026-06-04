package com.rally.user;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.TennisProfileVO;
import com.rally.domain.user.model.UserData;
import com.rally.user.convert.ProfileAppConvertMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
public class OnboardingAppService {

    @Resource
    private TennisProfileGateway tennisProfileGateway;

    @Resource
    private UserGateway userGateway;



    /**
     * 检查是否需要引导
     * 首次进入生成 tbc 记录，返回 needOnboarding=true
     */
    @Transactional
    public TennisProfileVO checkStatus() {
        String userId = UserContext.get();
        Optional<TennisProfileData> existing = tennisProfileGateway.findByUserId(userId);

        if (existing.isEmpty()) {
            // 首次进入，生成 tbc 记录
            TennisProfileData newData = buildDefaultProfile(userId);
            tennisProfileGateway.save(newData);
            return buildProfileVO(userId, newData);
        }

        return buildProfileVO(userId, existing.get());
    }

    /**
     * 提交 Onboarding，转 normal
     */
    @Transactional
    public TennisProfileVO submit(OnboardingCmd cmd) {
        String userId = UserContext.get();
        // 1. 校验必填项

        if (cmd.getNtrpScore() == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "NTRP 自评不能为空");
        }
        if (cmd.getCityCode() == null || cmd.getCityCode().isBlank()) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "城市不能为空");
        }
        if (cmd.getVideoKeys() == null || cmd.getVideoKeys().isEmpty()) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "至少上传一个视频");
        }

        // 2. 更新 users 表（性别/生日）
        UserData userData = userGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.DATA_NOT_FOUND, "用户不存在"));
        try {
            userData.setGender(cmd.getGender() == null ? null : GenderEnum.valueOf(cmd.getGender()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "性别值非法");
        }
        userData.setBirthday(cmd.getBirthday());
        userGateway.updateUser(userData);

        // 3. 更新 profile 表（ntrp/city/status=normal），不存在则创建
        boolean profileExists = tennisProfileGateway.findByUserId(userId).isPresent();
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElse(buildDefaultProfile(userId));
        profileData.setNtrpScore(cmd.getNtrpScore());
        profileData.setCityCode(cmd.getCityCode());
        profileData.setStatus(ProfileStatusEnum.NORMAL);
        profileData.setNtrpUpdatedAt(LocalDateTime.now());
        profileData.setVideoUrls(cmd.getVideoKeys());
        if (profileExists) {
            tennisProfileGateway.update(profileData);
        } else {
            tennisProfileGateway.save(profileData);
        }

        // 4. 写变更日志
        // 由 ProfileAppService 的 logChange 方法处理，这里简化

        return buildProfileVO(userId, profileData);
    }

    /**
     * 断言已完成 onboarding（供其他域调用）
     */
    public void assertOnboarded(String userId) {
        Optional<TennisProfileData> profile = tennisProfileGateway.findByUserId(userId);
        if (profile.isEmpty() || profile.get().getStatus() == ProfileStatusEnum.TBC) {
            throw new BusinessException(BizErrorCode.ONBOARDING_INCOMPLETE);
        }
    }

    /**
     * 构建默认 profile（TBC 状态，供首次创建使用）
     */
    private TennisProfileData buildDefaultProfile(String userId) {
        TennisProfileData data = new TennisProfileData();
        data.setUserId(userId);
        data.setStatus(ProfileStatusEnum.TBC);
        data.setReputationScore(new java.math.BigDecimal("100"));
        data.setCredibilityScore(new java.math.BigDecimal("0"));
        data.setCalibrationScore(new java.math.BigDecimal("80"));
        data.setIsUnderReview(false);
        data.setIsNewbie(true);
        data.setVideoUrls(new ArrayList<>());
        return data;
    }

    private TennisProfileVO buildProfileVO(String userId, TennisProfileData profileData) {
        UserData userData = userGateway.findByUserId(userId).orElse(null);
        return ProfileAppConvertMapper.INSTANCE.toMyProfileVO(profileData, userData, null, null, null);
    }
}
