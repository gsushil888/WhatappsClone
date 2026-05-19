package com.whatsapp.service.auth.strategy;

import com.whatsapp.dto.AuthDto;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.OtpVerification;
import com.whatsapp.entity.User;
import com.whatsapp.exception.AuthException;
import com.whatsapp.service.RateLimitService;
import com.whatsapp.service.auth.OtpHandler;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordLoginStrategy implements LoginStrategy {

	private final PasswordEncoder passwordEncoder;
	private final RateLimitService rateLimitService;
	private final OtpHandler otpHandler;

	@Override
	public AuthDto.LoginResponse execute(User user, AuthDto.LoginRequest request) {
		validatePassword(request.getPassword(), user.getPassword());

		String deviceFingerprint = RequestContext.getDeviceFingerprint();
		log.info("Password login for user {} with device fingerprint: {}", user.getId(), deviceFingerprint);
		rateLimitService.checkLoginRateLimit(user.getId(), deviceFingerprint);

		String contactInfo = "EMAIL".equals(request.getIdentifierType()) ? request.getIdentifier() : user.getEmail();
		return otpHandler.handleOtpLogin(contactInfo, OtpVerification.OtpType.EMAIL_LOGIN, user.getDisplayName());
	}

	@Override
	public boolean supports(String identifierType) {
		return "EMAIL".equals(identifierType) || "USERNAME".equals(identifierType);
	}

	private void validatePassword(String inputPassword, String userPassword) {
		if (inputPassword == null || inputPassword.trim().isEmpty()) {
			throw new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Password is required");
		}
		if (!passwordEncoder.matches(inputPassword, userPassword)) {
			throw new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials");
		}
	}
}


//package com.whatsapp.service.auth.strategy;
//
//import com.whatsapp.dto.AuthDto;
//import com.whatsapp.entity.ErrorCode;
//import com.whatsapp.entity.User;
//import com.whatsapp.exception.AuthException;
//import com.whatsapp.service.auth.SessionHandler;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class PasswordLoginStrategy implements LoginStrategy {
//
//	private final PasswordEncoder passwordEncoder;
//	private final SessionHandler sessionHandler;
//
//	@Override
//	public AuthDto.LoginResponse execute(User user, AuthDto.LoginRequest request) {
//		validatePassword(request.getPassword(), user.getPassword());
//
//		log.info("Password validated successfully for user: {}", user.getId());
//		return sessionHandler.createLoginResponse(user);
//	}
//
//	@Override
//	public boolean supports(String identifierType) {
//		return "EMAIL".equals(identifierType) || "USERNAME".equals(identifierType);
//	}
//
//	private void validatePassword(String inputPassword, String userPassword) {
//		if (inputPassword == null || inputPassword.trim().isEmpty()) {
//			throw new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Password is required");
//		}
//		if (!passwordEncoder.matches(inputPassword, userPassword)) {
//			throw new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials");
//		}
//	}
//}