package com.rally.personalprofile.genderupdate.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UpdateGenderCmd;
import com.rally.domain.user.model.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 update-gender：覆盖并保存当前用户的性别。
 */
@Component
@RequiredArgsConstructor
public class UpdateGenderActivity {

    private final UserRepository userRepository;

    public void execute(String userId, UpdateGenderCmd command) {
        // A1 用户编号只由已登录上下文传入；未找到时保留原 DATA_NOT_FOUND 契约。
        UserData user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.DATA_NOT_FOUND, "用户不存在"));

        // A2 请求层负责非空和枚举解析；活动直接覆盖并保存完整用户对象。
        user.setGender(command.getGender());
        userRepository.updateUser(user);
    }
}
