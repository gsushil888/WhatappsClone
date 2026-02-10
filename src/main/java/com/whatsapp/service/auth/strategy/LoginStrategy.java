package com.whatsapp.service.auth.strategy;

import com.whatsapp.dto.AuthDto;
import com.whatsapp.entity.User;

public interface LoginStrategy {
    AuthDto.LoginResponse execute(User user, AuthDto.LoginRequest request);
    boolean supports(String identifierType);
}