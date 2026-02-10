package com.whatsapp.service.auth;

import com.whatsapp.entity.ErrorCode;
import com.whatsapp.exception.AuthException;
import com.whatsapp.service.auth.strategy.LoginStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LoginStrategyFactory {
    
    private final List<LoginStrategy> strategies;
    
    public LoginStrategy getStrategy(String identifierType) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(identifierType))
            .findFirst()
            .orElseThrow(() -> new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Unsupported login type"));
    }
}