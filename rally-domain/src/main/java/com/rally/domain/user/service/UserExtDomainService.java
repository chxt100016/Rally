package com.rally.domain.user.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.user.enums.UserExtKeyEnum;
import com.rally.domain.user.gateway.UserExtRepository;
import com.rally.domain.user.model.UserExtData;
import com.rally.domain.utils.Assert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class UserExtDomainService {

    @Resource
    private UserExtRepository userExtRepository;

    public void save(String userId, String extKey, String extValue) {
        validateExtKey(extKey);
        Assert.notBlank(extValue, BizErrorCode.WECHAT_PAYMENT_CODE_EMPTY);

        UserExtData data = new UserExtData();
        data.setUserId(userId);
        data.setExtKey(extKey);
        data.setExtValue(extValue);
        userExtRepository.save(data);
    }

    public UserExtData get(String userId, String extKey) {
        validateExtKey(extKey);
        return userExtRepository.findByUserIdAndKey(userId, extKey).orElse(null);
    }

    public List<UserExtData> getAllByUserId(String userId) {
        return userExtRepository.findAllByUserId(userId);
    }

    public void delete(String userId, String extKey) {
        validateExtKey(extKey);
        UserExtData existing = userExtRepository.findByUserIdAndKey(userId, extKey).orElse(null);
        Assert.notNull(existing, BizErrorCode.USER_EXT_NOT_FOUND);
        userExtRepository.deleteByUserIdAndKey(userId, extKey);
    }

    private void validateExtKey(String extKey) {
        boolean isValid = Arrays.stream(UserExtKeyEnum.values()).anyMatch(e -> e.getKey().equals(extKey));
        Assert.isTrue(isValid, BizErrorCode.USER_EXT_KEY_INVALID);
    }
}
