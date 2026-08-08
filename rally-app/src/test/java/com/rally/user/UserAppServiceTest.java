package com.rally.user;

import com.rally.domain.auth.gateway.WechatClient;
import com.rally.domain.auth.model.WechatPhoneInfo;
import com.rally.domain.auth.model.WechatSession;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.service.UserDomainService;
import com.rally.user.model.WechatPhoneCodeCmd;
import com.rally.utils.UserContext;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class UserAppServiceTest {

    @Test
    public void saveWechatPhoneUpdatesCurrentUser() {
        RecordingUserRepository repository = new RecordingUserRepository();
        repository.user = new UserData();
        repository.user.setUserId("current-user");
        UserAppService service = new UserAppService(new RecordingWechatClient(), new UserDomainService(repository));
        WechatPhoneCodeCmd cmd = new WechatPhoneCodeCmd();
        cmd.setCode("dynamic-code");
        UserContext.set("current-user");

        try {
            service.saveWechatPhone(cmd);
        } finally {
            UserContext.clear();
        }

        assertEquals("current-user", repository.updated.getUserId());
        assertEquals("13800138000", repository.updated.getPhone());
    }

    private static class RecordingWechatClient implements WechatClient {
        @Override
        public WechatSession code2Session(String code) {
            return null;
        }

        @Override
        public WechatPhoneInfo getPhoneNumber(String code) {
            WechatPhoneInfo phoneInfo = new WechatPhoneInfo();
            phoneInfo.setPhoneNumber("13800138000");
            return phoneInfo;
        }
    }

    private static class RecordingUserRepository implements UserRepository {
        private UserData user;
        private UserData updated;

        @Override
        public UserData createUser(UserData user) {
            this.user = user;
            return user;
        }

        @Override
        public Optional<UserData> findByUserId(String userId) {
            return Optional.ofNullable(user != null && userId.equals(user.getUserId()) ? user : null);
        }

        @Override
        public void updateUser(UserData user) {
            updated = user;
        }
    }
}
