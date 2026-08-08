package com.rally.domain.user.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    @Transactional
    public void updatePhone(String userId, String phone) {
        Assert.notBlank(phone, BizErrorCode.PARAM_ERROR);
        UserData user = userRepository.findByUserId(userId).orElse(null);
        Assert.notNull(user, BizErrorCode.USER_NOT_EXIST);
        user.setPhone(phone);
        userRepository.updateUser(user);
    }
}
