package com.rally.domain.user.gateway;

import com.rally.domain.user.model.UserData;

import java.util.Optional;

public interface UserGateway {

    UserData createUser(UserData user);

    Optional<UserData> findByUserId(String userId);

    void updateUser(UserData user);

}
