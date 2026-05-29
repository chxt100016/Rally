package com.rally.domain.auth.gateway;

import com.rally.domain.auth.model.TokenPayload;

import java.util.Optional;

public interface TokenGateway {
    String issue(String userId);
    Optional<TokenPayload> verify(String token);
}
