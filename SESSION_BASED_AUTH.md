# Session-Based Authentication Implementation

## Overview
The authentication system now implements device-based session management to provide a seamless user experience while maintaining security.

## Key Features

### 1. Device-Based Session Management
- Each device gets a unique session based on device fingerprint
- Existing sessions are reused for same device logins
- New devices require OTP verification

### 2. Login Flow Logic

#### Scenario 1: First Time Login (New Device)
```
User Login → No Active Session → Password/OTP Required → Create New Session
```

#### Scenario 2: Same Device Login (Active Session Exists)
```
User Login → Active Session Found → Direct Login → Reuse Existing Session
```

#### Scenario 3: Different Device Login (User Has Active Sessions)
```
User Login → No Session on This Device → OTP Required → Create New Session
```

### 3. Session States
- **ACTIVE**: Session is valid and can be used
- **EXPIRED**: Session has expired naturally
- **REVOKED**: Session was manually terminated
- **TEMP**: Temporary session for OTP verification

## API Changes

### Login Request
```json
{
  "identifier": "user@example.com",
  "password": "password123"
}
```

### Login Response - Direct Login (Same Device)
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "requiresOtp": false,
    "user": { ... },
    "session": {
      "accessToken": "jwt_token",
      "refreshToken": "refresh_token",
      "expiresIn": 3600,
      "expiresAt": "2024-01-01T13:00:00Z"
    }
  }
}
```

### Login Response - OTP Required (New Device)
```json
{
  "success": true,
  "message": "OTP verification required",
  "data": {
    "requiresOtp": true,
    "tempSessionId": "temp_abc123",
    "otpSentTo": "us***@example.com",
    "expiresIn": 300,
    "expiresAt": "2024-01-01T12:05:00Z"
  }
}
```

### Logout Request Options

#### Logout Current Session Only
```json
{
  "logoutAllDevices": false
}
```

#### Logout All Sessions
```json
{
  "logoutAllDevices": true
}
```

#### Logout Specific Session
```json
{
  "logoutAllDevices": false,
  "sessionId": "session_xyz789"
}
```

## Implementation Details

### AuthService Changes

1. **login()** method now checks for existing active sessions
2. **createDirectLoginResponse()** handles same-device login
3. **handlePasswordLoginNewDevice()** manages new device authentication
4. **revokeSpecificSession()** supports targeted session termination

### Database Queries

New repository methods for session management:
- `findByUserIdAndDeviceFingerprintAndStatusAndExpiresAtAfter()`
- `findActiveSessionsByUserId()`
- Session-specific revocation methods

### Security Benefits

1. **Reduced OTP Friction**: Known devices don't require OTP
2. **Multi-Device Support**: Each device maintains independent session
3. **Granular Control**: Users can logout specific devices
4. **Session Isolation**: Compromised device doesn't affect others

## Usage Examples

### Client Implementation

```javascript
// Login attempt
const loginResponse = await fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Device-Fingerprint': generateDeviceFingerprint()
  },
  body: JSON.stringify({
    identifier: 'user@example.com',
    password: 'password123'
  })
});

const data = await loginResponse.json();

if (data.data.requiresOtp) {
  // Show OTP input
  showOtpVerification(data.data.tempSessionId);
} else {
  // Direct login successful
  storeTokens(data.data.session);
}
```

### Device Fingerprint Generation

```javascript
function generateDeviceFingerprint() {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  ctx.textBaseline = 'top';
  ctx.font = '14px Arial';
  ctx.fillText('Device fingerprint', 2, 2);
  
  return btoa(JSON.stringify({
    screen: `${screen.width}x${screen.height}`,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    language: navigator.language,
    platform: navigator.platform,
    canvas: canvas.toDataURL()
  }));
}
```

## Testing Scenarios

1. **First Login**: New user, new device → OTP required
2. **Same Device**: Existing user, same device → Direct login
3. **New Device**: Existing user, new device → OTP required
4. **Session Expiry**: Expired session → OTP required
5. **Logout All**: All sessions terminated → OTP required for all devices
6. **Logout Specific**: One session terminated → Other devices unaffected

## Configuration

```yaml
session:
  max-per-user: 5
  default-expiry: 2592000000 # 30 days
  require-otp-after-logout: true
  device-fingerprint-required: true
```

This implementation provides WhatsApp-like authentication behavior where users can seamlessly switch between trusted devices while maintaining security for new device access.