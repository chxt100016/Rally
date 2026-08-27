package com.rally.personalprofile.basicprofileupdate.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.EditProfileCmd;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 update-basic-profile：按非空请求字段更新当前用户基础资料。
 */
@Component
@RequiredArgsConstructor
public class UpdateBasicProfileActivity {

    private final UserRepository userRepository;

    public void execute(String userId, EditProfileCmd command) {
        // A1 用户编号只由已登录上下文传入；未找到时保留原 DATA_NOT_FOUND 契约。
        UserData user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.DATA_NOT_FOUND, "用户不存在"));

        // A2 通过 @identity.user C2 执行非 null 合并；空字符串是有效新值，不做内容预校验。
        UserProfile aggregate = UserProfile.create(user, null);
        aggregate.updateBasicProfile(command);

        // A3 空请求也保存完整资料；数据库容量或写入异常交由统一系统异常处理。
        userRepository.updateUser(aggregate.getUser());
    }
}
