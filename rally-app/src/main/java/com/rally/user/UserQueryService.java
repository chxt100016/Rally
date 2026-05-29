package com.rally.user;

import com.rally.auth.convert.AuthConvertMapper;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.UserVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {

    @Resource
    private UserGateway userGateway;

    public UserVO getCurrentUser(String userId) {
        return userGateway.findByUserId(userId)
                .map(AuthConvertMapper.INSTANCE::toVO)
                .orElseThrow(() -> new AuthException(10004, "用户不存在"));
    }
}
