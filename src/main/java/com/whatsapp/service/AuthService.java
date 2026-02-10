package com.whatsapp.service;

import com.whatsapp.dto.AuthDto;
import com.whatsapp.dto.MediaDto;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.OtpVerification;
import com.whatsapp.entity.PrivacySettings;
import com.whatsapp.entity.User;
import com.whatsapp.entity.UserSession;
import com.whatsapp.exception.AuthException;
import com.whatsapp.repository.OtpVerificationRepository;
import com.whatsapp.repository.UserRepository;
import com.whatsapp.repository.UserSessionRepository;
import com.whatsapp.service.auth.LoginStrategyFactory;
import com.whatsapp.service.auth.SessionHandler;
import com.whatsapp.util.JwtUtil;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final UserSessionRepository userSessionRepository;
	private final OtpService otpService;
	private final OtpVerificationRepository otpRepository;
	private final EmailService emailService;
	private final MediaService mediaService;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final SessionService sessionService;
	private final LoginStrategyFactory loginStrategyFactory;
	private final SessionHandler sessionHandler;

	@Transactional
	public AuthDto.RegisterResponse register(AuthDto.RegisterRequest request, MultipartFile profilePicture) {
		String username = generateOrValidateUsername(request);

		validateUserNotExists(request.getEmail(), request.getPhoneNumber(), username);

		String profilePictureUrl = uploadProfilePicture(profilePicture);
		User user = createUser(request, username, profilePictureUrl);
		userRepository.save(user);

		return sendRegistrationOtp(user, username);
	}

	@Transactional
	public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
		String identifier = Optional.ofNullable(request.getIdentifier()).orElseThrow(
				() -> new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Email, mobile, or username is required"));

		User user = userRepository.findByIdentifierAndStatus(identifier)
				.orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

		// Check whether user is active or not
		validateUserStatus(user);

		return loginStrategyFactory.getStrategy(request.getIdentifierType()).execute(user, request);
	}

	@Transactional
	public AuthDto.LoginResponse verifyOtp(AuthDto.VerifyOtpRequest request) {
		OtpVerification otp = otpService.verifyOtp(request.getTempSessionId(), request.getOtpCode());

		User user = userRepository.findByIdentifierAndStatus(otp.getContactInfo())
				.orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

		validateUserStatus(user);
		updateUserAfterOtpVerification(user, otp);

		return sessionHandler.createLoginResponse(user);
	}

	@Transactional
	public AuthDto.RefreshTokenResponse refreshToken(AuthDto.RefreshTokenRequest request) {
		validateRefreshToken(request.getRefreshToken());

		Long userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());
		String sessionId = jwtUtil.getSessionIdFromToken(request.getRefreshToken());

		UserSession session = userSessionRepository.findById(sessionId)
				.filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == UserSession.SessionStatus.ACTIVE)
				.orElseThrow(() -> new AuthException(ErrorCode.AUTH_SESSION_EXPIRED));

		return updateSessionTokens(session, userId, sessionId);
	}

	@Transactional
	public void logout(Long userId, AuthDto.LogoutRequest request) {
		if (request.getLogoutAllDevices()) {
			revokeAllUserSessions(userId);
			log.info("All sessions revoked for user: {}", userId);
		} else if (request.getSessionId() != null && !request.getSessionId().trim().isEmpty()) {
			revokeSpecificSession(userId, request.getSessionId());
			log.info("Specific session {} revoked for user: {}", request.getSessionId(), userId);
		} else {
			revokeCurrentSession();
			log.info("Current session revoked for user: {}", userId);
		}
	}

	@Transactional
	public AuthDto.RegisterResponse resendOtp(AuthDto.ResendOtpRequest request) {
		OtpVerification oldOtp = otpRepository.findByTempSessionId(request.getTempSessionId())
				.orElseThrow(() -> new AuthException(ErrorCode.AUTH_OTP_INVALID, "Invalid session"));

		User user = userRepository.findByIdentifierAndStatus(oldOtp.getContactInfo())
				.orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

		String tempSessionId = generateTempSessionId();
		String otpCode = otpService.generateOtp(tempSessionId, oldOtp.getContactInfo(), oldOtp.getOtpType());
		emailService.sendOtpEmail(oldOtp.getContactInfo(), otpCode, user.getDisplayName());

		return AuthDto.RegisterResponse.builder().tempSessionId(tempSessionId)
				.otpSentTo(maskEmail(oldOtp.getContactInfo())).expiresIn(60)
				.expiresAt(LocalDateTime.now().plusMinutes(1)).resendAvailableIn(60).username(user.getUsername())
				.build();
	}

	private AuthDto.LoginResponse handleOtpLogin(User user, AuthDto.LoginRequest request) {
		String identifier = request.getIdentifier();
		String identifierType = request.getIdentifierType();
		String contactInfo;
		OtpVerification.OtpType otpType;
		String maskedContact;

		if ("EMAIL".equals(identifierType)) {
			contactInfo = identifier;
			otpType = OtpVerification.OtpType.EMAIL_LOGIN;
			maskedContact = maskEmail(identifier);
		} else if ("PHONE".equals(identifierType)) {
			contactInfo = identifier;
			otpType = OtpVerification.OtpType.PHONE_LOGIN;
			maskedContact = maskPhone(identifier);
		} else {
			if (user.getEmail() != null && !user.getEmail().isEmpty()) {
				contactInfo = user.getEmail();
				otpType = OtpVerification.OtpType.EMAIL_LOGIN;
				maskedContact = maskEmail(user.getEmail());
			} else {
				contactInfo = user.getPhoneNumber();
				otpType = OtpVerification.OtpType.PHONE_LOGIN;
				maskedContact = maskPhone(user.getPhoneNumber());
			}
		}

		String deviceFingerprint = RequestContext.getDeviceFingerprint();
		Optional<OtpVerification> existingOtp = otpService.findActiveOtpByDevice(contactInfo, otpType,
				deviceFingerprint);

		if (existingOtp.isPresent()) {
			OtpVerification otp = existingOtp.get();
			long timeLeft = java.time.Duration.between(LocalDateTime.now(), otp.getExpiresAt()).getSeconds();

			if (timeLeft > 0) {
				return AuthDto.LoginResponse.builder().requiresOtp(true).tempSessionId(otp.getTempSessionId())
						.otpSentTo(maskedContact).expiresIn((int) timeLeft).expiresAt(otp.getExpiresAt())
						.message("Please wait. OTP already sent to " + maskedContact).build();
			}
		}

		String tempSessionId = generateTempSessionId();
		String otpCode = otpService.generateOtp(tempSessionId, contactInfo, otpType);

		if (otpType == OtpVerification.OtpType.EMAIL_LOGIN) {
			emailService.sendOtpEmail(contactInfo, otpCode, user.getDisplayName());
		}

		return AuthDto.LoginResponse.builder().requiresOtp(true).tempSessionId(tempSessionId).otpSentTo(maskedContact)
				.expiresIn(60).expiresAt(LocalDateTime.now().plusMinutes(1)).message("OTP sent to " + maskedContact)
				.build();
	}

	private AuthDto.LoginResponse createDirectLoginResponse(User user, UserSession existingSession) {
		String accessToken = jwtUtil.generateAccessToken(user.getId(), existingSession.getId());
		String refreshToken = jwtUtil.generateRefreshToken(user.getId(), existingSession.getId());

		sessionService.updateSessionTokens(existingSession, accessToken, refreshToken);

		user.setOnline(true);
		user.setLastActiveAt(LocalDateTime.now());
		userRepository.save(user);

		return AuthDto.LoginResponse.builder().requiresOtp(false).user(mapToUserInfo(user))
				.session(AuthDto.SessionInfo.builder().accessToken(accessToken).refreshToken(refreshToken)
						.expiresIn(3600).expiresAt(LocalDateTime.now().plusHours(1)).build())
				.build();
	}

	private AuthDto.LoginResponse createLoginResponse(User user) {
		UserSession session = sessionService.createSession(user);
		String accessToken = jwtUtil.generateAccessToken(user.getId(), session.getId());
		String refreshToken = jwtUtil.generateRefreshToken(user.getId(), session.getId());

		sessionService.updateSessionTokens(session, accessToken, refreshToken);

		return AuthDto.LoginResponse.builder().requiresOtp(false).user(mapToUserInfo(user))
				.session(AuthDto.SessionInfo.builder().accessToken(accessToken).refreshToken(refreshToken)
						.expiresIn(3600).expiresAt(LocalDateTime.now().plusHours(1)).build())
				.build();
	}

	private void validateUserStatus(User user) {
		if (user.getStatus() != User.UserStatus.ACTIVE) {
			throw new AuthException(ErrorCode.AUTH_ACCOUNT_SUSPENDED);
		}
	}

	private void validateRefreshToken(String refreshToken) {
		if (!jwtUtil.validateToken(refreshToken)) {
			throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN);
		}
	}

	private void validateUserNotExists(String email, String phoneNumber, String username) {
		if (userRepository.existsByEmailOrPhoneNumberOrUsername(email, phoneNumber, username)) {
			throw new AuthException(ErrorCode.USER_ALREADY_EXISTS);
		}
	}

	private String generateOrValidateUsername(AuthDto.RegisterRequest request) {
		if (request.getUserName() != null && !request.getUserName().trim().isEmpty()) {
			String username = request.getUserName().trim();
			if (userRepository.existsByUsername(username)) {
				throw new AuthException(ErrorCode.USER_ALREADY_EXISTS, "Username not available, try another");
			}
			return username;
		}

		String firstName = request.getFirstName();
		String lastName = request.getLastName();

		if ((firstName == null || firstName.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
			return generateUniqueUsername("user", "");
		}

		String prefix = "";
		String name = "";

		if (lastName != null && !lastName.trim().isEmpty()) {
			prefix = lastName.trim().toLowerCase().substring(0, Math.min(2, lastName.trim().length()));
		}

		if (firstName != null && !firstName.trim().isEmpty()) {
			name = firstName.trim().toLowerCase().replaceAll("\\s+", "");
		} else if (lastName != null && !lastName.trim().isEmpty()) {
			name = lastName.trim().toLowerCase().replaceAll("\\s+", "");
		}

		return generateUniqueUsername(prefix, name);
	}

	private String generateUniqueUsername(String prefix, String name) {
		String baseUsername = prefix + name;
		String username;
		int attempts = 0;

		do {
			int randomDigits = (int) (Math.random() * (99999 - 100 + 1)) + 100;
			username = baseUsername + randomDigits;
			attempts++;
		} while (userRepository.existsByUsername(username) && attempts < 10);

		if (userRepository.existsByUsername(username)) {
			username = baseUsername + UUID.randomUUID().toString().substring(0, 8).replaceAll("-", "");
		}

		return username;
	}

	private String uploadProfilePicture(MultipartFile profilePicture) {
		return Optional.ofNullable(profilePicture).filter(file -> !file.isEmpty())
				.map(file -> mediaService.uploadMedia(null, file, "PROFILE_PICTURE", null, true, true))
				.map(MediaDto.MediaUploadResponse::getFileUrl).orElse(null);
	}

	private User createUser(AuthDto.RegisterRequest request, String username, String profilePictureUrl) {
		User user = User.builder().username(username).email(request.getEmail()).phoneNumber(request.getPhoneNumber())
				.password(passwordEncoder.encode(request.getPassword())).firstName(request.getFirstName())
				.lastName(request.getLastName()).displayName(request.getDisplayName()).aboutText(request.getAboutText())
				.profilePictureUrl(profilePictureUrl).status(User.UserStatus.ACTIVE)
				.accountType(User.AccountType.PERSONAL).isOnline(false).isVerified(false).emailVerified(false)
				.phoneVerified(false).build();

		user.setPrivacySettings(PrivacySettings.builder().user(user).build());
		return user;
	}

	private AuthDto.RegisterResponse sendRegistrationOtp(User user, String generatedUsername) {
		String tempSessionId = generateTempSessionId();
		String otpCode = otpService.generateOtp(tempSessionId, user.getEmail(), OtpVerification.OtpType.REGISTRATION);
		emailService.sendOtpEmail(user.getEmail(), otpCode, user.getDisplayName());

		return AuthDto.RegisterResponse.builder().tempSessionId(tempSessionId).otpSentTo(maskEmail(user.getEmail()))
				.expiresIn(60).expiresAt(LocalDateTime.now().plusMinutes(1)).resendAvailableIn(60)
				.username(generatedUsername).build();
	}

	private void updateUserAfterOtpVerification(User user, OtpVerification otp) {
		user.setVerified(true);
		user.setOnline(true);
		user.setLastSeenAt(LocalDateTime.now());
		user.setLastActiveAt(LocalDateTime.now());

		if (otp.getOtpType() == OtpVerification.OtpType.EMAIL_LOGIN || otp.getContactInfo().contains("@")) {
			user.setEmailVerified(true);
		} else {
			user.setPhoneVerified(true);
		}

		userRepository.save(user);
	}

	private AuthDto.RefreshTokenResponse updateSessionTokens(UserSession session, Long userId, String sessionId) {
		String newAccessToken = jwtUtil.generateAccessToken(userId, sessionId);
		String newRefreshToken = jwtUtil.generateRefreshToken(userId, sessionId);

		sessionService.updateSessionTokens(session, newAccessToken, newRefreshToken);

		return AuthDto.RefreshTokenResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken)
				.expiresIn(3600).expiresAt(LocalDateTime.now().plusHours(1)).build();
	}

	private void revokeAllUserSessions(Long userId) {
		sessionService.revokeAllUserSessions(userId);
	}

	private void revokeCurrentSession() {
		Optional.ofNullable(RequestContext.getSessionId()).ifPresent(sessionService::revokeSession);
	}

	private void revokeSpecificSession(Long userId, String sessionId) {
		userSessionRepository.findById(sessionId).filter(session -> session.getUser().getId().equals(userId))
				.ifPresentOrElse(session -> sessionService.revokeSession(sessionId), () -> {
					log.warn("Session {} not found or doesn't belong to user {}", sessionId, userId);
					throw new AuthException(ErrorCode.AUTH_SESSION_NOT_FOUND);
				});
	}

	private AuthDto.UserInfo mapToUserInfo(User user) {
		return AuthDto.UserInfo.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
				.phoneNumber(user.getPhoneNumber()).displayName(user.getDisplayName())
				.profilePictureUrl(user.getProfilePictureUrl()).aboutText(user.getAboutText()).isOnline(user.isOnline())
				.lastSeenAt(user.getLastSeenAt()).accountType(user.getAccountType().name())
				.isVerified(user.isVerified()).build();
	}

	private String generateTempSessionId() {
		return "temp_" + UUID.randomUUID().toString().replace("-", "");
	}

	private String maskEmail(String email) {
		int atIndex = email.indexOf('@');
		return atIndex > 2 ? email.substring(0, 2) + "***" + email.substring(atIndex)
				: "***" + email.substring(atIndex);
	}

	private String maskPhone(String phone) {
		return phone.length() > 4 ? phone.substring(0, 2) + "***" + phone.substring(phone.length() - 2) : "***";
	}
}