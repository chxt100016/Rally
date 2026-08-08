package com.rally.domain.user.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserDomainServiceTest {

    @Test
    public void updatePhoneUpdatesExistingUser() {
        RecordingUserRepository repository = new RecordingUserRepository();
        repository.user = user("user-1", "球员");
        UserDomainService service = new UserDomainService(repository);

        service.updatePhone("user-1", "13800138000");

        assertEquals("13800138000", repository.updated.getPhone());
        assertEquals("球员", repository.updated.getNickname());
    }

    @Test
    public void updatePhoneRejectsMissingUser() {
        RecordingUserRepository repository = new RecordingUserRepository();
        UserDomainService service = new UserDomainService(repository);

        try {
            service.updatePhone("missing", "13800138000");
        } catch (BusinessException exception) {
            assertEquals(BizErrorCode.USER_NOT_EXIST, exception.getErrorCode());
            assertNull(repository.updated);
            return;
        }
        throw new AssertionError("Expected BusinessException");
    }

    @Test
    public void updatePhoneRejectsBlankPhone() {
        RecordingUserRepository repository = new RecordingUserRepository();
        repository.user = user("user-1", "球员");
        UserDomainService service = new UserDomainService(repository);

        try {
            service.updatePhone("user-1", " ");
        } catch (BusinessException exception) {
            assertEquals(BizErrorCode.PARAM_ERROR, exception.getErrorCode());
            assertNull(repository.updated);
            return;
        }
        throw new AssertionError("Expected BusinessException");
    }

    private UserData user(String userId, String nickname) {
        UserData user = new UserData();
        user.setUserId(userId);
        user.setNickname(nickname);
        return user;
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
            return Optional.ofNullable(user);
        }

        @Override
        public void updateUser(UserData user) {
            updated = user;
        }
    }
}
