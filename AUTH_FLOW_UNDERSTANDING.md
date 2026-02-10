# WhatsApp Clone - Authentication & Authorization Flow

## Table of Contents
1. [Overview](#overview)
2. [Authentication Flow](#authentication-flow)
3. [Authorization Flow](#authorization-flow)
4. [Component Details](#component-details)
5. [Security Mechanisms](#security-mechanisms)

---

## Overview

### Authentication vs Authorization
- **Authentication**: Verifying WHO you are (Login process)
- **Authorization**: Verifying WHAT you can access (Permission checking)

### Key Components
```
Client → Filters → Controller → Service → Repository → Database
         ↓
    Security Context
```

---

## Authentication Flow

### 1. Registration Flow

```
Client Request
    ↓
POST /api/v1/auth/register
    ↓
RequestContextFilter (extracts device info, IP, User-Agent)
    ↓
AuthController.register()
    ↓
AuthService.register()
    ↓
├─ Validate user doesn't exist
├─ Upload profile picture (MediaService)
├─ Create User entity
├─ Save to database (UserRepository)
└─ Generate OTP (OtpService)
    ↓
EmailService.sendOtpEmail()
    ↓
Response: { tempSessionId, otpSentTo, expiresIn }
```

#### Classes Involved:
- **RequestContextFilter**: Extracts headers, device fingerprint, IP
- **AuthController**: HTTP endpoint handler
- **AuthService**: Business logic orchestration
- **MediaService**: Profile picture upload
- **OtpService**: OTP generation and storage
- **EmailService**: Email sending
- **UserRepository**: Database operations

#### Data Flow:
```java
// Request
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "displayName": "John Doe"
}

// OTP Record Created
OtpVerification {
  tempSessionId: "temp_abc123",
  contactInfo: "john@example.com",
  otpCode: "123456",
  otpType: EMAIL_REGISTRATION,
  deviceFingerprint: "fp_12345",
  expiresAt: now + 1 minute
}
```

---

### 2. Login Flow (Password-Based)

```
Client Request
    ↓
POST /api/v1/auth/login
Headers: X-Device-Fingerprint
    ↓
RequestContextFilter
├─ Extract X-Device-Fingerprint → x_device_fingerprint
├─ Extract IP address
├─ Extract User-Agent
└─ Store in RequestContext (ThreadLocal)
    ↓
AuthController.login()
    ↓
AuthService.login()
├─ Find user by identifier (email/username)
├─ Validate user status (ACTIVE)
└─ Delegate to strategy
    ↓
LoginStrategyFactory.getStrategy("EMAIL")
    ↓
PasswordLoginStrategy.execute()
├─ Validate password (BCrypt)
├─ Get device fingerprint from RequestContext
└─ Check for existing session
    ↓
SessionService.findActiveSessionByDevice()
    ↓
┌─────────────────────────────────────┐
│  Session Exists?                    │
├─────────────────────────────────────┤
│ YES → Direct Login                  │
│  ├─ Generate new JWT tokens         │
│  ├─ Update session tokens           │
│  ├─ Set user online                 │
│  └─ Return login response           │
│                                     │
│ NO → OTP Required                   │
│  ├─ Check rate limit                │
│  ├─ Generate OTP                    │
│  ├─ Send email                      │
│  └─ Return OTP response             │
└─────────────────────────────────────┘
```

#### Classes Involved:
- **RequestContextFilter**: Request preprocessing
- **AuthController**: HTTP handling
- **AuthService**: Main orchestration
- **LoginStrategyFactory**: Strategy pattern for login types
- **PasswordLoginStrategy**: Password validation logic
- **SessionService**: Session management
- **SessionHandler**: Session response creation
- **OtpHandler**: OTP flow management
- **RateLimitService**: Rate limiting
- **JwtUtil**: Token generation

#### Device Fingerprint Flow:
```
Client sends: X-Device-Fingerprint: "device_abc123"
    ↓
RequestContextFilter extracts and stores as: x_device_fingerprint
    ↓
RequestContext.getDeviceFingerprint() retrieves it
    ↓
Used for session lookup and creation
```

---

### 3. OTP Verification Flow

```
Client Request
    ↓
POST /api/v1/auth/otp/verify
{
  "tempSessionId": "temp_abc123",
  "otpCode": "123456"
}
    ↓
AuthController.verifyOtp()
    ↓
AuthService.verifyOtp()
    ↓
OtpService.verifyOtp()
├─ Find OTP by tempSessionId
├─ Check expiration
├─ Check attempts count
├─ Validate OTP code
└─ Mark as VERIFIED
    ↓
Find user by contact info
    ↓
Update user (set online, verified)
    ↓
SessionHandler.createLoginResponse()
    ↓
SessionService.createSession()
├─ Get device fingerprint
├─ Check for existing session
├─ Create new session if needed
│   ├─ Generate UUID for session ID
│   ├─ Extract device info
│   ├─ Set expiry (30 days)
│   └─ Save to database
└─ Return session
    ↓
JwtUtil.generateAccessToken()
├─ Claims: { sessionId, tokenType: "ACCESS" }
├─ Subject: userId
├─ Expiry: 15 minutes
└─ Sign with HS256
    ↓
JwtUtil.generateRefreshToken()
├─ Claims: { sessionId, tokenType: "REFRESH" }
├─ Subject: userId
├─ Expiry: 24 hours
└─ Sign with HS256
    ↓
Update session with tokens
    ↓
Send welcome email
    ↓
Response: { user, session: { accessToken, refreshToken } }
```

#### Session Creation Details:
```java
UserSession {
  id: "ba31d9a1-f4cd-4adb-9f77-59e765050a99",
  userId: 2,
  deviceFingerprint: "device_abc123",
  deviceType: "WEB",
  deviceOs: "Windows",
  deviceBrowser: "Chrome",
  ipAddress: "192.168.1.1",
  userAgent: "Mozilla/5.0...",
  status: ACTIVE,
  expiresAt: now + 30 days,
  createdAt: now
}
```

---

### 4. Token Refresh Flow

```
Client Request
    ↓
POST /api/v1/auth/refresh
{
  "refreshToken": "eyJ..."
}
    ↓
AuthController.refreshToken()
    ↓
AuthService.refreshToken()
    ↓
JwtUtil.validateToken(refreshToken)
├─ Verify signature
├─ Check expiration
└─ Return true/false
    ↓
Extract userId and sessionId from token
    ↓
UserSessionRepository.findById(sessionId)
├─ Check session exists
├─ Check status == ACTIVE
└─ Check userId matches
    ↓
Generate new tokens
├─ New access token (15 min)
└─ New refresh token (24 hours)
    ↓
Update session with new tokens
    ↓
Response: { accessToken, refreshToken, expiresIn }
```

---

### 5. Logout Flow

```
Client Request
    ↓
POST /api/v1/auth/logout
Headers: Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter (validates token first)
    ↓
AuthController.logout()
    ↓
AuthService.logout()
    ↓
┌─────────────────────────────────────┐
│  Logout Type?                       │
├─────────────────────────────────────┤
│ All Devices                         │
│  └─ SessionService.revokeAllUserSessions() │
│     └─ Set all sessions to REVOKED  │
│                                     │
│ Specific Session                    │
│  └─ SessionService.revokeSession(sessionId) │
│     └─ Set session to REVOKED       │
│                                     │
│ Current Session                     │
│  └─ Get sessionId from RequestContext │
│  └─ SessionService.revokeSession()  │
└─────────────────────────────────────┘
```

---

## Authorization Flow

### 1. Request Authorization

```
Client Request
    ↓
GET /api/v1/users/me
Headers: Authorization: Bearer <accessToken>
    ↓
RequestContextFilter
├─ Extract correlation ID
├─ Extract device info
├─ Extract IP, User-Agent
└─ Set RequestContext
    ↓
JwtAuthenticationFilter
├─ Extract token from Authorization header
├─ Validate token signature & expiry
├─ Extract userId and sessionId
├─ Check session status in database
│   └─ Query: SELECT status FROM user_sessions WHERE id = ?
├─ If session.status == ACTIVE:
│   ├─ Set userId in RequestContext
│   ├─ Set sessionId in RequestContext
│   └─ Set Authentication in SecurityContext
└─ If session.status == REVOKED:
    └─ Skip authentication (401 Unauthorized)
    ↓
SecurityFilterChain
├─ Check if endpoint requires authentication
├─ Check SecurityContext has authentication
└─ Allow/Deny request
    ↓
Controller Method
├─ @AuthenticationPrincipal gets userId
└─ Process request
```

#### JWT Authentication Filter Details:
```java
// Token Validation Steps
1. Extract token from "Bearer <token>"
2. JwtUtil.validateToken(token)
   - Verify signature with secret key
   - Check expiration date
3. Extract claims:
   - userId from "sub"
   - sessionId from "sessionId"
4. Database check:
   - Query session by sessionId
   - Verify status == ACTIVE
5. Set security context:
   - UsernamePasswordAuthenticationToken
   - Principal: userId
   - Authorities: [ROLE_USER]
```

---

### 2. Security Configuration

```java
SecurityFilterChain:
  ↓
RequestContextFilter (Order: 1)
  ↓
JwtAuthenticationFilter (Order: 2)
  ↓
SecurityFilterChain Rules:
  ├─ /api/v1/auth/** → permitAll()
  ├─ /api/** → authenticated()
  ├─ /ws/** → permitAll()
  └─ anyRequest() → authenticated()
```

#### Filter Order:
1. **RequestContextFilter**: Sets up request context
2. **JwtAuthenticationFilter**: Validates JWT and sets authentication
3. **Spring Security Filters**: Authorization checks

---

## Component Details

### 1. RequestContextFilter
**Purpose**: Extract and store request metadata

**Extracts**:
- Device fingerprint from `X-Device-Fingerprint` header
- Device info (type, OS, browser, model)
- IP address (X-Forwarded-For, X-Real-IP, or remote address)
- User-Agent
- Correlation ID

**Storage**: ThreadLocal `RequestContext`

**Key Methods**:
```java
extractDeviceInfo() → Map<String, String>
extractHeaders() → Map<String, String>
extractClientIpAddress() → String
```

---

### 2. JwtAuthenticationFilter
**Purpose**: Validate JWT tokens and set authentication

**Process**:
1. Extract token from Authorization header
2. Validate token (signature + expiry)
3. Check session status in database
4. Set authentication in SecurityContext

**Dependencies**:
- JwtUtil: Token validation
- UserSessionRepository: Session status check

---

### 3. AuthService
**Purpose**: Main authentication orchestration

**Methods**:
- `login()`: Handle login requests
- `verifyOtp()`: Verify OTP and create session
- `refreshToken()`: Generate new tokens
- `logout()`: Revoke sessions
- `register()`: User registration

**Dependencies**:
- UserRepository
- UserSessionRepository
- OtpService
- EmailService
- SessionService
- JwtUtil

---

### 4. SessionService
**Purpose**: Session lifecycle management

**Methods**:
- `createSession()`: Create new session with device info
- `findActiveSessionByDevice()`: Find existing session
- `updateSessionTokens()`: Update tokens and activity
- `revokeSession()`: Revoke specific session
- `revokeAllUserSessions()`: Revoke all user sessions

**Key Logic**:
```java
Device Fingerprint Priority:
1. X-Device-Fingerprint header
2. Device info from headers
3. Fallback: hash(IP + UserAgent)
```

---

### 5. OtpService
**Purpose**: OTP generation and verification

**Methods**:
- `generateOtp()`: Create 6-digit OTP
- `verifyOtp()`: Validate OTP code
- `findActiveOtpByDevice()`: Check existing OTP

**OTP Lifecycle**:
```
Generate → PENDING (60 sec)
Verify → VERIFIED
Expire → EXPIRED
Max attempts → FAILED
```

---

### 6. JwtUtil
**Purpose**: JWT token operations

**Token Structure**:
```json
{
  "sessionId": "uuid",
  "tokenType": "ACCESS|REFRESH",
  "sub": "userId",
  "iat": timestamp,
  "exp": timestamp
}
```

**Methods**:
- `generateAccessToken()`: 15 minutes expiry
- `generateRefreshToken()`: 24 hours expiry
- `validateToken()`: Signature + expiry check
- `getUserIdFromToken()`: Extract user ID
- `getSessionIdFromToken()`: Extract session ID

---

## Security Mechanisms

### 1. Device Fingerprinting
**Purpose**: Identify unique devices for session management

**Sources** (Priority order):
1. Client-provided: `X-Device-Fingerprint` header
2. Fallback: `hash(IP + UserAgent)`

**Usage**:
- Session creation and lookup
- OTP device binding
- Multi-device session management

---

### 2. Session Management
**Features**:
- Device-based sessions
- 30-day expiry
- Active/Revoked status
- Token binding

**Session States**:
- `ACTIVE`: Valid session
- `REVOKED`: Logged out
- `EXPIRED`: Time expired
- `TEMP`: Temporary (not used)

---

### 3. Token Security
**Access Token**:
- Short-lived (15 minutes)
- Used for API requests
- Contains sessionId for validation

**Refresh Token**:
- Long-lived (24 hours)
- Used to get new access tokens
- Bound to session

**Validation**:
1. Signature verification (HS256)
2. Expiration check
3. Session status check (database)

---

### 4. Rate Limiting
**Purpose**: Prevent brute force attacks

**Implementation**: RateLimitService
- Checks login attempts per device
- Configurable limits
- Time-based windows

---

### 5. OTP Security
**Features**:
- 6-digit random code
- 60-second expiry
- Max 3 attempts
- Device-bound
- One-time use

**Protection**:
- Rate limiting
- Attempt tracking
- Automatic expiration
- Device fingerprint binding

---

## Data Flow Summary

### Login with Existing Session
```
Client → Filter → Controller → Service → Strategy
  → SessionService.findActiveSessionByDevice()
  → Generate new tokens
  → Update session
  → Return response
```

### Login without Session (First Time)
```
Client → Filter → Controller → Service → Strategy
  → SessionService.findActiveSessionByDevice() [Not Found]
  → OtpHandler.handleOtpLogin()
  → Generate OTP
  → Send email
  → Return OTP response
  ↓
Client verifies OTP
  ↓
Service → SessionService.createSession()
  → Generate tokens
  → Return response
```

### Subsequent API Requests
```
Client → RequestContextFilter → JwtAuthenticationFilter
  → Validate token
  → Check session status
  → Set authentication
  → Controller → Service → Repository
```

### Logout
```
Client → Filter → Controller → Service
  → SessionService.revokeSession()
  → Update status to REVOKED
  ↓
Next request with same token
  → JwtAuthenticationFilter
  → Session status check fails
  → 401 Unauthorized
```

---

## Key Takeaways

1. **Stateless JWT + Stateful Sessions**: JWT tokens are stateless but validated against database sessions
2. **Device Fingerprinting**: Enables multi-device support and session management
3. **Two-Factor Flow**: Password + OTP for first login, direct login for subsequent requests
4. **Token Refresh**: Separate refresh tokens for security
5. **Session Revocation**: Logout immediately invalidates tokens via database check
6. **ThreadLocal Context**: Request metadata available throughout request lifecycle
7. **Filter Chain**: RequestContext → JWT → Security → Controller
8. **Consistent Fingerprints**: Fallback uses hash of IP+UserAgent for consistency

