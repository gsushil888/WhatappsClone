package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.AuthDto;
import com.whatsapp.service.AuthService;
import com.whatsapp.service.MediaService;
import com.whatsapp.util.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MediaService mediaService;

    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<AuthDto.RegisterResponse>> register(
            @Valid @ModelAttribute AuthDto.RegisterRequest request,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture) {
        
        log.info("User registration attempt for email: {}", request.getEmail());
        AuthDto.RegisterResponse response = authService.register(request, profilePicture);
        
        return ResponseEntity.ok(ApiResponse.<AuthDto.RegisterResponse>builder()
                .success(true)
                .message("Registration initiated. Please verify OTP.")
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        
        log.info("Login attempt with identifier type: {}, device fingerprint: {}", 
                request.getIdentifierType(), RequestContext.getDeviceFingerprint());
        AuthDto.LoginResponse response = authService.login(request);
        
        String message;
        if (response.getRequiresOtp()) {
            message = "OTP verification required";
        } else if (response.getUser() != null && response.getSession() != null) {
            message = "Already logged in - session refreshed";
        } else {
            message = "Login successful";
        }
        
        return ResponseEntity.ok(ApiResponse.<AuthDto.LoginResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> verifyOtp(
            @Valid @RequestBody AuthDto.VerifyOtpRequest request) {

        log.info("OTP verification for session: {}", request.getTempSessionId());
        AuthDto.LoginResponse response = authService.verifyOtp(request);
        
        return ResponseEntity.ok(ApiResponse.<AuthDto.LoginResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {

        log.info("Token refresh request");
        AuthDto.RefreshTokenResponse response = authService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.<AuthDto.RefreshTokenResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            @RequestBody(required = false) AuthDto.LogoutRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        AuthDto.LogoutRequest logoutRequest = request != null ? request
                : AuthDto.LogoutRequest.builder().logoutAllDevices(false).build();

        log.info("Logout request for user: {}, allDevices: {}", userId, logoutRequest.getLogoutAllDevices());
        authService.logout(userId, logoutRequest);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logged out successfully")
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<AuthDto.RegisterResponse>> resendOtp(
            @Valid @RequestBody AuthDto.ResendOtpRequest request) {
        
        log.info("Resend OTP request for session: {}", request.getTempSessionId());
        AuthDto.RegisterResponse response = authService.resendOtp(request);
        
        return ResponseEntity.ok(ApiResponse.<AuthDto.RegisterResponse>builder()
                .success(true)
                .message("OTP resent successfully")
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }
}