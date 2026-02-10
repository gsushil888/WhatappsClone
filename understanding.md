# WhatsApp Clone Authentication & Session Management

## Overview

This document explains the comprehensive authentication system that handles device-based login, session management, OTP verification, and security measures similar to production WhatsApp.

## Authentication Flow

### 1. First-Time Login (New Device)

```
User → Login Request → OTP Required → OTP Verification → Session Created → Access Granted
```

**Steps:**
1. User provides email/mobile/username + password
2. System detects new device (no existing session)
3. **OTP Required**: System sends OTP to email/mobile
4. User verifies OTP
5. System creates new session with device fingerprint
6. JWT tokens issued (access + refresh)
7. User gains access

### 2. Subsequent Logins (Known Device)

```
User → Login Request → Token Validation → Access Granted (No OTP)
```

**Steps:**
1. User provides credentials
2. System recognizes device fingerprint
3. **No OTP Required**: Valid session exists
4. JWT tokens refreshed
5. User gains immediate access

### 3. Logout Process

```
User → Logout → Session Invalidated → Next Login Requires OTP
```

**Steps:**
1. User initiates logout
2. Session marked as `REVOKED` in database
3. JWT tokens blacklisted in Redis
4. Device fingerprint cleared
5. **Next login requires OTP verification**

## Device-Based Session Management

### Device Fingerprinting

The system creates unique device fingerprints using:

```javascript
// Client sends these headers
{
  "X-Device-Type": "WEB",
  "X-Device-OS": "Windows",
  "X-Device-OS-Version": "10",
  "X-App-Version": "1.0.0",
  "X-Device-Model": "Chrome",
  "X-Device-Fingerprint": "unique_device_hash"
}
```

### Session States

```java
public enum SessionStatus {
    ACTIVE,    // Valid session, no OTP required
    EXPIRED,   // Session expired, OTP required
    REVOKED,   // Manually logged out, OTP required
    TEMP       // Temporary session during OTP flow
}
```

### Multi-Device Support

- **Same User, Multiple Devices**: Each device has separate session
- **Device Limit**: Maximum 5 active sessions per user
- **Session Priority**: Newest session can revoke oldest if limit exceeded

## Filter-Based Token Validation

### 1. RequestContextFilter

**Purpose**: Extract device information and set request context

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain chain) {
    
    // Extract device info from headers
    Map<String, String> deviceInfo = extractDeviceInfo(request);
    String deviceFingerprint = deviceInfo.get("device_fingerprint");
    
    // Set request context
    RequestContext.setContext(RequestInfo.builder()
        .deviceInfo(deviceInfo)
        .ipAddress(extractClientIpAddress(request))
        .userAgent(request.getHeader("User-Agent"))
        .build());
    
    chain.doFilter(request, response);
}
```

### 2. JwtAuthenticationFilter

**Purpose**: Validate JWT tokens and check session status

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain chain) {
    
    String token = extractToken(request);
    if (token != null && jwtUtil.validateToken(token)) {
        
        Long userId = jwtUtil.getUserIdFromToken(token);
        String sessionId = jwtUtil.getSessionIdFromToken(token);
        
        // Check session status in database
        UserSession session = userSessionRepository.findById(sessionId);
        
        if (session != null && session.getStatus() == SessionStatus.ACTIVE) {
            // Valid session - set authentication
            setAuthentication(userId, sessionId);
        } else {
            // Invalid session - require re-authentication
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
    }
    
    chain.doFilter(request, response);
}
```

## Session Security Features

### 1. Duplicate Session Prevention

```java
public UserSession createSession(User user) {
    String deviceFingerprint = RequestContext.getDeviceFingerprint();
    
    // Check for existing session on same device
    Optional<UserSession> existingSession = userSessionRepository
        .findByUserIdAndDeviceFingerprint(user.getId(), deviceFingerprint);
    
    if (existingSession.isPresent()) {
        // Revoke old session
        existingSession.get().setStatus(SessionStatus.REVOKED);
        userSessionRepository.save(existingSession.get());
    }
    
    // Create new session
    return UserSession.builder()
        .user(user)
        .deviceFingerprint(deviceFingerprint)
        .status(SessionStatus.ACTIVE)
        .build();
}
```

### 2. Session Validation

```java
public boolean isValidSession(String sessionId, String deviceFingerprint) {
    UserSession session = userSessionRepository.findById(sessionId);
    
    return session != null && 
           session.getStatus() == SessionStatus.ACTIVE &&
           session.getDeviceFingerprint().equals(deviceFingerprint) &&
           session.getExpiresAt().isAfter(LocalDateTime.now());
}
```

### 3. Concurrent Session Management

```java
public void enforceSessionLimit(Long userId) {
    List<UserSession> activeSessions = userSessionRepository
        .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.ACTIVE);
    
    if (activeSessions.size() > MAX_SESSIONS_PER_USER) {
        // Revoke oldest sessions
        activeSessions.stream()
            .skip(MAX_SESSIONS_PER_USER)
            .forEach(session -> {
                session.setStatus(SessionStatus.REVOKED);
                userSessionRepository.save(session);
            });
    }
}
```

## Authentication Scenarios

### Scenario 1: New User Registration

```
1. User registers → OTP sent
2. User verifies OTP → Account activated
3. Session created with device fingerprint
4. Subsequent logins on same device → No OTP required
```

### Scenario 2: Existing User, New Device

```
1. User logs in from new device → OTP required
2. User verifies OTP → New session created
3. Both old and new device sessions remain active
4. Each device can login without OTP until logout
```

### Scenario 3: User Logs Out

```
1. User clicks logout → Session revoked
2. Device fingerprint cleared from active sessions
3. Next login from same device → OTP required again
```

### Scenario 4: Session Expiry

```
1. Session expires (30 days default)
2. User tries to access → Token invalid
3. User must login again → OTP required
```

### Scenario 5: Suspicious Activity

```
1. Login from new location/device
2. Additional security checks triggered
3. OTP required regardless of device history
4. Email notification sent to user
```

## Implementation Details

### Database Schema

```sql
-- User Sessions Table
CREATE TABLE user_sessions (
    id VARCHAR(100) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(255),
    device_info JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(1000),
    status ENUM('ACTIVE', 'EXPIRED', 'REVOKED', 'TEMP'),
    jwt_token VARCHAR(500),
    refresh_token VARCHAR(500),
    expires_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Redis Cache Structure

```
# Session validation cache
session:{sessionId} → {userId, deviceFingerprint, status, expiresAt}

# OTP storage
otp:{tempSessionId} → {otpCode, contactInfo, expiresAt, attempts}

# Blacklisted tokens
blacklist:token:{tokenHash} → {revokedAt, reason}
```

### Configuration

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  access-token-validity: 900000    # 15 minutes
  refresh-token-validity: 86400000 # 24 hours

# Session Configuration
session:
  max-per-user: 5
  default-expiry: 2592000000 # 30 days
  require-otp-after-logout: true
  device-fingerprint-required: true

# OTP Configuration
otp:
  expiry: 300000 # 5 minutes
  max-attempts: 3
  resend-cooldown: 60000 # 1 minute
```

## Security Benefits

### 1. **Device Trust Management**
- Known devices don't require OTP
- Unknown devices always require OTP
- Device fingerprinting prevents session hijacking

### 2. **Session Isolation**
- Each device has independent session
- Logout from one device doesn't affect others
- Granular session control

### 3. **Brute Force Protection**
- OTP attempts limited
- Account lockout after failed attempts
- Rate limiting on login endpoints

### 4. **Token Security**
- Short-lived access tokens (15 minutes)
- Refresh tokens for seamless experience
- Token blacklisting on logout

### 5. **Audit Trail**
- All login attempts logged
- Device information tracked
- Session activity monitored

## Client Integration

### Login Flow (JavaScript Example)

```javascript
// 1. Initial login
const loginResponse = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-Device-Fingerprint': generateDeviceFingerprint(),
        'X-Device-Type': 'WEB',
        'X-Device-OS': navigator.platform
    },
    body: JSON.stringify({
        email: 'user@example.com',
        password: 'password123'
    })
});

if (loginResponse.requiresOtp) {
    // 2. OTP verification required
    const otpResponse = await fetch('/api/v1/auth/verify-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            tempSessionId: loginResponse.tempSessionId,
            otpCode: '123456'
        })
    });
    
    // Store tokens
    localStorage.setItem('accessToken', otpResponse.session.accessToken);
    localStorage.setItem('refreshToken', otpResponse.session.refreshToken);
} else {
    // Direct access granted (known device)
    localStorage.setItem('accessToken', loginResponse.session.accessToken);
    localStorage.setItem('refreshToken', loginResponse.session.refreshToken);
}
```

This comprehensive system ensures security while providing a smooth user experience, similar to how WhatsApp handles authentication across multiple devices.

---

## UML-Style Request Flow Diagrams

### 1. First-Time Login Flow (New Device - OTP Required)

```
Client                    Filters                   Controllers              Services                 Database/Cache
  |                         |                          |                       |                         |
  |-- POST /auth/login ---->|                          |                       |                         |
  |   Headers:              |                          |                       |                         |
  |   X-Device-Fingerprint  |                          |                       |                         |
  |   X-Device-Type         |                          |                       |                         |
  |   Body: {email,pwd}     |                          |                       |                         |
  |                         |                          |                       |                         |
  |                    RequestContextFilter            |                       |                         |
  |                         |-- extractDeviceInfo() ---|                       |                         |
  |                         |-- setRequestContext() ---|                       |                         |
  |                         |                          |                       |                         |
  |                    JwtAuthenticationFilter         |                       |                         |
  |                         |-- extractToken() --------|                       |                         |
  |                         |-- (no token, skip) ------|                       |                         |
  |                         |                          |                       |                         |
  |                         |                    AuthController               |                         |
  |                         |                          |-- login() ----------->|                         |
  |                         |                          |                  AuthService                    |
  |                         |                          |                       |-- extractIdentifier() --|                         |
  |                         |                          |                       |-- detectIdentifierType()|                         |
  |                         |                          |                       |-- findUser() ---------->|-- SELECT users WHERE email=? -->
  |                         |                          |                       |<-- User entity ---------|<-- User data ---------------|
  |                         |                          |                       |-- validateUserStatus() -|                         |
  |                         |                          |                       |-- checkDeviceSession() >|-- SELECT user_sessions WHERE -->
  |                         |                          |                       |<-- No active session ---|<-- device_fingerprint=? -------|
  |                         |                          |                       |-- handleOtpLogin() -----|                         |
  |                         |                          |                       |                    OtpService                    |
  |                         |                          |                       |-- generateOtp() ------->|-- SET otp:tempId {code,exp} -->
  |                         |                          |                       |<-- tempSessionId -------|<-- Redis confirmation ---------|
  |                         |                          |                       |                    EmailService                  |
  |                         |                          |                       |-- sendOtpEmail() ------>|-- SMTP send email ----------->
  |                         |                          |                       |<-- email sent ----------|<-- Email delivered ------------|
  |                         |                          |<-- LoginResponse -----|                         |
  |                         |                          |   {requiresOtp:true,  |                         |
  |                         |                          |    tempSessionId}     |                         |
  |<-- 200 OK --------------|<-------------------------|<----------------------|                         |
  |   {requiresOtp: true,   |                          |                       |                         |
  |    tempSessionId: "..."}|                          |                       |                         |
  |                         |                          |                       |                         |
  |-- POST /auth/verify-otp>|                          |                       |                         |
  |   Body: {tempSessionId, |                          |                       |                         |
  |          otpCode}       |                          |                       |                         |
  |                         |                          |                       |                         |
  |                    RequestContextFilter            |                       |                         |
  |                         |-- setRequestContext() ---|                       |                         |
  |                         |                          |                       |                         |
  |                         |                    AuthController               |                         |
  |                         |                          |-- verifyOtp() ------->|                         |
  |                         |                          |                  AuthService                    |
  |                         |                          |                       |                    OtpService                    |
  |                         |                          |                       |-- verifyOtp() --------->|-- GET otp:tempId ----------->
  |                         |                          |                       |<-- OtpVerification -----|<-- {code,contact,exp} --------|
  |                         |                          |                       |-- validateOtp() --------|-- DEL otp:tempId ----------->
  |                         |                          |                       |-- findUser() ---------->|-- SELECT users WHERE email=? -->
  |                         |                          |                       |<-- User entity ---------|<-- User data ---------------|
  |                         |                          |                       |-- updateUserStatus() -->|-- UPDATE users SET verified=1,|
  |                         |                          |                       |                         |   isOnline=1, lastSeenAt=NOW()|
  |                         |                          |                       |-- createSession() ----->|-- INSERT user_sessions ----->
  |                         |                          |                       |<-- UserSession ---------|<-- Session created -----------|
  |                         |                          |                       |                    JwtUtil                      |
  |                         |                          |                       |-- generateTokens() ----->|-- Create JWT tokens -------->
  |                         |                          |                       |<-- {access,refresh} -----|<-- Signed tokens ------------|
  |                         |                          |                       |-- saveTokens() -------->|-- UPDATE user_sessions ----->
  |                         |                          |                       |                         |   SET jwt_token=?, refresh=? |
  |                         |                          |<-- LoginResponse -----|                         |
  |                         |                          |   {requiresOtp:false, |                         |
  |                         |                          |    user, session}     |                         |
  |<-- 200 OK --------------|<-------------------------|<----------------------|                         |
  |   {user: {...},         |                          |                       |                         |
  |    session: {           |                          |                       |                         |
  |      accessToken: "...",|                          |                       |                         |
  |      refreshToken: "..."}|                          |                       |                         |
```

### 2. Subsequent Login Flow (Known Device - No OTP)

```
Client                    Filters                   Controllers              Services                 Database/Cache
  |                         |                          |                       |                         |
  |-- POST /auth/login ---->|                          |                       |                         |
  |   Headers:              |                          |                       |                         |
  |   X-Device-Fingerprint  |                          |                       |                         |
  |   (same as before)      |                          |                       |                         |
  |                         |                          |                       |                         |
  |                    RequestContextFilter            |                       |                         |
  |                         |-- extractDeviceInfo() ---|                       |                         |
  |                         |-- setRequestContext() ---|                       |                         |
  |                         |                          |                       |                         |
  |                         |                    AuthController               |                         |
  |                         |                          |-- login() ----------->|                         |
  |                         |                          |                  AuthService                    |
  |                         |                          |                       |-- findUser() ---------->|-- SELECT users WHERE email=? -->
  |                         |                          |                       |<-- User entity ---------|<-- User data ---------------|
  |                         |                          |                       |-- checkDeviceSession() >|-- SELECT user_sessions WHERE -->
  |                         |                          |                       |<-- Active session ------|<-- user_id=? AND device_fp=? --|
  |                         |                          |                       |                         |   AND status='ACTIVE' ---------|
  |                         |                          |                       |-- handleDirectLogin() --|                         |
  |                         |                          |                       |-- refreshTokens() ----->|-- UPDATE user_sessions ----->
  |                         |                          |                       |                         |   SET last_activity=NOW() ----|
  |                         |                          |<-- LoginResponse -----|                         |
  |                         |                          |   {requiresOtp:false, |                         |
  |                         |                          |    user, session}     |                         |
  |<-- 200 OK --------------|<-------------------------|<----------------------|                         |
  |   {user: {...},         |                          |                       |                         |
  |    session: {           |                          |                       |                         |
  |      accessToken: "...",|                          |                       |                         |
  |      refreshToken: "..."}|                          |                       |                         |
```

### 3. Protected API Request Flow

```
Client                    Filters                   Controllers              Services                 Database/Cache
  |                         |                          |                       |                         |
  |-- GET /users/me ------->|                          |                       |                         |
  |   Headers:              |                          |                       |                         |
  |   Authorization: Bearer |                          |                       |                         |
  |   X-Device-Fingerprint  |                          |                       |                         |
  |                         |                          |                       |                         |
  |                    RequestContextFilter            |                       |                         |
  |                         |-- extractDeviceInfo() ---|                       |                         |
  |                         |-- setRequestContext() ---|                       |                         |
  |                         |                          |                       |                         |
  |                    JwtAuthenticationFilter         |                       |                         |
  |                         |-- extractToken() --------|                       |                         |
  |                         |                    JwtUtil                       |                         |
  |                         |-- validateToken() ------>|-- verify signature -->|-- Check token expiry ------->
  |                         |<-- token valid ----------|<-- JWT validated -----|<-- Token not expired --------|
  |                         |-- getUserId() ---------->|-- decode claims ----->|                         |
  |                         |<-- userId --------------- |<-- userId extracted ---|                         |
  |                         |-- getSessionId() ------->|-- decode claims ----->|                         |
  |                         |<-- sessionId ------------|<-- sessionId extracted |                         |
  |                         |-- validateSession() ---->|                       |-- SELECT user_sessions WHERE -->
  |                         |<-- session valid --------|                       |<-- id=? AND status='ACTIVE' --|
  |                         |-- setAuthentication() ---|                       |                         |
  |                         |                          |                       |                         |
  |                         |                    UserController               |                         |
  |                         |                          |-- getCurrentUser() -->|                         |
  |                         |                          |                  UserService                    |
  |                         |                          |                       |-- getUserProfile() ---->|-- SELECT users WHERE id=? --->
  |                         |                          |                       |<-- User entity ---------|<-- User data with privacy ----|
  |                         |                          |                       |-- mapToResponse() ------|                         |
  |                         |                          |<-- UserProfileResp ---|                         |
  |<-- 200 OK --------------|<-------------------------|<----------------------|                         |
  |   {id, username,        |                          |                       |                         |
  |    displayName, ...}    |                          |                       |                         |
```

### 4. Logout Flow

```
Client                    Filters                   Controllers              Services                 Database/Cache
  |                         |                          |                       |                         |
  |-- POST /auth/logout --->|                          |                       |                         |
  |   Headers:              |                          |                       |                         |
  |   Authorization: Bearer |                          |                       |                         |
  |   Body: {               |                          |                       |                         |
  |     logoutAllDevices    |                          |                       |                         |
  |   }                     |                          |                       |                         |
  |                         |                          |                       |                         |
  |                    RequestContextFilter            |                       |                         |
  |                         |-- setRequestContext() ---|                       |                         |
  |                         |                          |                       |                         |
  |                    JwtAuthenticationFilter         |                       |                         |
  |                         |-- validateToken() -------|                       |                         |
  |                         |-- setAuthentication() ---|                       |                         |
  |                         |                          |                       |                         |
  |                         |                    AuthController               |                         |
  |                         |                          |-- logout() ---------->|                         |
  |                         |                          |                  AuthService                    |
  |                         |                          |                       |-- getSessionId() -------|                         |
  |                         |                          |                       |-- revokeSession() ----->|-- UPDATE user_sessions ----->
  |                         |                          |                       |                         |   SET status='REVOKED' -------|
  |                         |                          |                       |                         |   WHERE id=? -----------------|
  |                         |                          |                       |-- blacklistToken() ---->|-- SET blacklist:token:hash --->
  |                         |                          |                       |                         |   {revokedAt, reason} --------|
  |                         |                          |                       |-- clearDeviceCache() -->|-- DEL session:sessionId ------>
  |                         |                          |<-- Success ----------|                         |
  |<-- 200 OK --------------|<-------------------------|<----------------------|                         |
  |   {success: true,       |                          |                       |                         |
  |    message: "Logged out"}|                          |                       |                         |
```

### 5. Token Refresh Flow

```
Client                    Filters                   Controllers              Services                 Database/Cache
  |                         |                          |                       |                         |
  |-- POST /auth/refresh -->|                          |                       |                         |
  |   Body: {               |                          |                       |                         |
  |     refreshToken: "..." |                          |                       |                         |
  |   }                     |                          |                       |                         |
  |                         |                          |                       |                         |
  |                    RequestContextFilter            |                       |                         |
  |                         |-- setRequestContext() ---|                       |                         |
  |                         |                          |                       |                         |
  |                         |                    AuthController               |                         |
  |                         |                          |-- refreshToken() ---->|                         |
  |                         |                          |                  AuthService                    |
  |                         |                          |                       |                    JwtUtil                      |
  |                         |                          |                       |-- validateToken() ----->|-- verify refresh token ------>
  |                         |                          |                       |<-- token valid ---------|<-- signature & expiry valid --|
  |                         |                          |                       |-- getUserId() --------->|-- decode claims ------------->
  |                         |                          |                       |<-- userId --------------|<-- userId extracted ----------|
  |                         |                          |                       |-- getSessionId() ------>|-- decode claims ------------->
  |                         |                          |                       |<-- sessionId -----------|<-- sessionId extracted -------|
  |                         |                          |                       |-- validateSession() --->|-- SELECT user_sessions WHERE -->
  |                         |                          |                       |<-- session active ------|<-- id=? AND status='ACTIVE' --|
  |                         |                          |                       |-- generateNewTokens() ->|-- create new JWT tokens ----->
  |                         |                          |                       |<-- {access,refresh} ----|<-- signed tokens -------------|
  |                         |                          |                       |-- updateSession() ----->|-- UPDATE user_sessions ----->
  |                         |                          |                       |                         |   SET jwt_token=?, refresh=?,|
  |                         |                          |                       |                         |   last_activity=NOW() --------|
  |                         |                          |<-- RefreshResponse ---|                         |
  |<-- 200 OK --------------|<-------------------------|<----------------------|                         |
  |   {accessToken: "...",  |                          |                       |                         |
  |    refreshToken: "...", |                          |                       |                         |
  |    expiresIn: 900}      |                          |                       |                         |
```

### 6. Session Validation on Every Request

```
Every Protected API Request:

Client Request
     ↓
RequestContextFilter
     ↓ (extract device info)
JwtAuthenticationFilter
     ↓ (validate token)
Check Session Status in DB
     ↓
┌─────────────────────────────────────┐
│ Session Status Check:               │
│                                     │
│ ACTIVE    → Allow request           │
│ EXPIRED   → Return 401 Unauthorized │
│ REVOKED   → Return 401 Unauthorized │
│ NOT_FOUND → Return 401 Unauthorized │
└─────────────────────────────────────┘
     ↓
Controller → Service → Database
     ↓
Response to Client
```

### 7. Device Fingerprint Validation

```
Request with Token:

Extract Device Fingerprint from Headers
     ↓
Extract Session ID from JWT Token
     ↓
Query Database: SELECT device_fingerprint FROM user_sessions WHERE id = sessionId
     ↓
┌─────────────────────────────────────┐
│ Device Fingerprint Match:           │
│                                     │
│ MATCH     → Continue processing     │
│ NO_MATCH  → Revoke session & 401    │
│ MISSING   → Require re-auth & 401   │
└─────────────────────────────────────┘
```

### 8. Multi-Device Session Management

```
New Login Request:

Check Existing Sessions for User
     ↓
┌─────────────────────────────────────┐
│ Session Count Check:                │
│                                     │
│ < 5 Sessions  → Create new session  │
│ = 5 Sessions  → Revoke oldest       │
│ > 5 Sessions  → Revoke excess       │
└─────────────────────────────────────┘
     ↓
Create New Session with Device Fingerprint
     ↓
Return Success Response
```

These diagrams show the complete request flow from client to database, including all filters, controllers, services, and data persistence layers involved in the authentication and session management system.