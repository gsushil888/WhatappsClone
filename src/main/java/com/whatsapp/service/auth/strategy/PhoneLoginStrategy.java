package com.whatsapp.service.auth.strategy;

import com.whatsapp.dto.AuthDto;
import com.whatsapp.entity.OtpVerification;
import com.whatsapp.entity.User;
import com.whatsapp.service.OtpService;
import com.whatsapp.service.auth.OtpHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhoneLoginStrategy implements LoginStrategy {

  private final OtpHandler otpHandler;

  @Override
  public AuthDto.LoginResponse execute(User user, AuthDto.LoginRequest request) {
    return otpHandler.handleOtpLogin(request.getIdentifier(), OtpVerification.OtpType.PHONE_LOGIN,
        user.getDisplayName());
  }

  @Override
  public boolean supports(String identifierType) {
    return "PHONE".equals(identifierType);
  }
}
