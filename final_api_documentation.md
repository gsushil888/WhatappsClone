# WhatsApp System Final API Documentation

## API Overview

### Base URL: `https://api.whatsapp.com`
### WebSocket URL: `wss://api.whatsapp.com/ws`
### STOMP Endpoint: `/ws/stomp`

---

## 1. Authentication APIs

### 1.1 User Registration
**POST** `/api/v1/auth/register`

**Headers (Client sends):**
```json
{
  "Content-Type": "application/json",
  "X-Device-Type": "mobile",
  "X-Device-OS": "android",
  "X-Device-OS-Version": "12.0",
  "X-App-Version": "1.0.0",
  "X-Device-Model": "Samsung Galaxy S21",
  "X-Device-Fingerprint": "device_abc123",
  "User-Agent": "WhatsApp/1.0.0 (Android 12; Samsung SM-G991B)"
}
```

**Server Processing:**
```java
// RequestContextFilter extracts headers into Map
Map<String, String> headers = extractAllHeaders(request);
Map<String, String> requestInfo = Map.of(
    "deviceType", headers.getOrDefault("x-device-type", "unknown"),
    "os", headers.getOrDefault("x-device-os", "unknown"),
    "osVersion", headers.getOrDefault("x-device-os-version", "unknown"),
    "appVersion", headers.getOrDefault("x-app-version", "unknown"),
    "deviceModel", headers.getOrDefault("x-device-model", "unknown"),
    "deviceFingerprint", headers.getOrDefault("x-device-fingerprint", "unknown"),
    "userAgent", headers.getOrDefault("user-agent", "unknown"),
    "ipAddress", getClientIpAddress(request)
);
RequestContext.setRequestInfo(requestInfo);
```

**Request Body (Only user data):**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "phoneNumber": "+1234567890",
  "password": "securePassword123",
  "displayName": "John Doe",
  "aboutText": "Hey there! I am using WhatsApp."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registration initiated. Please verify OTP.",
  "data": {
    "tempSessionId": "temp_abc123def456",
    "otpSentTo": "jo***@example.com",
    "expiresIn": 300,
    "expiresAt": "2024-01-15T10:35:00Z",
    "resendAvailableIn": 60
  },
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 1.2 User Login (Universal Identifier)
**POST** `/api/v1/auth/login`

**Headers (Client sends):**
```json
{
  "Content-Type": "application/json",
  "X-Device-Type": "mobile",
  "X-Device-Fingerprint": "device_abc123",
  "X-Device-OS": "android",
  "User-Agent": "WhatsApp/1.0.0"
}
```

**Request Body Examples:**
```json
// Email login (OTP only - no password)
{
  "email": "john@example.com"
}

// Phone login (OTP only - no password)
{
  "mobile": "+1234567890"
}

// Username login (requires password)
{
  "username": "john_doe",
  "password": "securePassword123"
}
```

**Server Processing:**
```java
// AuthService determines auth method based on which field is provided
public String extractIdentifier(LoginRequest request) {
    if (request.getEmail() != null) return request.getEmail();
    if (request.getMobile() != null) return request.getMobile();
    if (request.getUsername() != null) return request.getUsername();
    throw new AuthException("Email, mobile, or username is required");
}

// Authentication logic based on identifier type
String identifier = extractIdentifier(request);
String identifierType = detectIdentifierType(identifier);
if ("USERNAME".equals(identifierType)) {
    // Username + Password (Direct login - no OTP)
    return handleUsernameLogin(user, request.getPassword());
} else {
    // Email/Phone only - no password needed, direct OTP
    return handleOtpLogin(user, identifierType);
}
```

**Response for Email/Phone (OTP Required):**
```json
{
  "success": true,
  "data": {
    "requiresOtp": true,
    "tempSessionId": "temp_xyz789",
    "otpSentTo": "jo***@example.com",
    "expiresIn": 300,
    "expiresAt": "2024-01-15T10:35:00Z"
  }
}
```

**Response for Username (Direct Login):**
```json
{
  "success": true,
  "data": {
    "requiresOtp": false,
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "phoneNumber": "+1234567890",
      "displayName": "John Doe",
      "profilePictureUrl": "https://cdn.example.com/profiles/john.jpg",
      "aboutText": "Hey there! I am using WhatsApp.",
      "isOnline": true,
      "lastSeenAt": "2024-01-15T10:25:00Z",
      "accountType": "PERSONAL",
      "isVerified": true
    },
    "session": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresIn": 3600,
      "expiresAt": "2024-01-15T11:35:00Z"
    }
  }
}
```

### 1.3 OTP Verification
**POST** `/api/v1/auth/verify-otp`

**Request Body:**
```json
{
  "tempSessionId": "temp_abc123def456",
  "otpCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "phoneNumber": "+1234567890",
      "displayName": "John Doe",
      "profilePictureUrl": "https://cdn.example.com/profiles/john.jpg",
      "aboutText": "Hey there! I am using WhatsApp.",
      "isOnline": true,
      "lastSeenAt": "2024-01-15T10:25:00Z",
      "accountType": "PERSONAL",
      "isVerified": true
    },
    "session": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresIn": 3600,
      "expiresAt": "2024-01-15T11:25:00Z"
    }
  },
  "correlationId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### 1.4 Refresh Token
**POST** `/api/v1/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "expiresAt": "2024-01-15T11:30:00Z"
  }
}
```

### 1.5 Logout
**POST** `/api/v1/auth/logout`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**JWT Token Payload (Server extracts):**
```json
{
  "userId": 1,
  "sessionId": "sess_abc123def456",
  "deviceFingerprint": "device_abc123",
  "iat": 1642248000,
  "exp": 1642251600
}
```

**Request Body:**
```json
{
  "logoutAllDevices": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

## 2. User Management APIs

### 2.1 Get Current User Profile
**GET** `/api/v1/users/me`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "phoneNumber": "+1234567890",
    "displayName": "John Doe",
    "profilePictureUrl": "https://cdn.example.com/profiles/john.jpg",
    "aboutText": "Hey there! I am using WhatsApp.",
    "isOnline": true,
    "lastSeenAt": "2024-01-15T10:25:00Z",
    "isVerified": true,
    "accountType": "PERSONAL",
    "privacySettings": {
      "lastSeenVisibility": "CONTACTS",
      "profilePhotoVisibility": "EVERYONE",
      "statusVisibility": "CONTACTS",
      "readReceiptsEnabled": true,
      "groupsVisibility": "CONTACTS"
    }
  }
}
```

### 2.2 Update User Profile
**PUT** `/api/v1/users/me`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "Content-Type": "application/json"
}
```

**Request Body:**
```json
{
  "displayName": "John Smith",
  "aboutText": "Busy working on something awesome!",
  "profilePhoto": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "displayName": "John Smith",
    "aboutText": "Busy working on something awesome!",
    "profilePictureUrl": "https://cdn.example.com/profiles/john_updated.jpg"
  }
}
```

### 2.3 Universal Search
**GET** `/api/v1/search?q=john&limit=20&offset=0&type=all`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Query Parameters:**
- `q` - Search query (required)
- `type` - Search scope: `all`, `contacts`, `chats`, `messages`, `users` (default: `all`)
- `limit` - Results per category (default: 20)
- `offset` - Skip results (default: 0)

**Response:**
```json
{
  "success": true,
  "data": {
    "contacts": [
      {
        "id": 1,
        "type": "CONTACT",
        "contactUser": {
          "id": 2,
          "displayName": "John Smith",
          "profilePictureUrl": "https://cdn.example.com/profiles/john2.jpg",
          "isOnline": true
        },
        "displayName": "Johnny",
        "matchedField": "displayName"
      }
    ],
    "conversations": [
      {
        "id": 3,
        "type": "CONVERSATION",
        "name": "John's Group",
        "profileImageUrl": "https://cdn.example.com/groups/john_group.jpg",
        "conversationType": "GROUP",
        "lastMessage": {
          "content": "Hey everyone!",
          "timestamp": "2024-01-15T10:20:00Z"
        },
        "matchedField": "name"
      }
    ],
    "messages": [
      {
        "id": 100,
        "type": "MESSAGE",
        "content": "John mentioned this yesterday",
        "timestamp": "2024-01-15T09:30:00Z",
        "conversation": {
          "id": 1,
          "name": "Jane Doe",
          "type": "INDIVIDUAL"
        },
        "sender": {
          "id": 2,
          "displayName": "Jane Doe"
        },
        "matchedField": "content"
      }
    ],
    "users": [
      {
        "id": 4,
        "type": "USER",
        "username": "john_wilson",
        "displayName": "John Wilson",
        "profilePictureUrl": "https://cdn.example.com/profiles/john4.jpg",
        "isOnline": false,
        "accountType": "PERSONAL",
        "matchedField": "username"
      }
    ],
    "summary": {
      "totalContacts": 1,
      "totalConversations": 1,
      "totalMessages": 1,
      "totalUsers": 1,
      "hasMore": false
    }
  }
}
```

---

## 3. Contact Management APIs

### 3.1 Get Contacts
**GET** `/api/v1/contacts?limit=50&offset=0`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "contacts": [
      {
        "id": 1,
        "contactUser": {
          "id": 2,
          "username": "jane_doe",
          "displayName": "Jane Doe",
          "profilePictureUrl": "https://cdn.example.com/profiles/jane.jpg",
          "isOnline": true,
          "lastSeenAt": "2024-01-15T10:20:00Z"
        },
        "displayName": "Jane",
        "isBlocked": false,
        "isFavorite": true,
        "status": "ACTIVE",
        "createdAt": "2024-01-10T08:00:00Z"
      }
    ],
    "pagination": {
      "total": 1,
      "limit": 50,
      "offset": 0,
      "hasMore": false
    }
  }
}
```

### 3.2 Add Contact
**POST** `/api/v1/contacts`

**Request Body:**
```json
{
  "contactUserId": 3,
  "displayName": "Bob Wilson"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "contactUser": {
      "id": 3,
      "username": "bob_wilson",
      "displayName": "Bob Wilson",
      "profilePictureUrl": "https://cdn.example.com/profiles/bob.jpg"
    },
    "displayName": "Bob Wilson",
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 3.3 Block Contact
**POST** `/api/v1/contacts/{contactId}/block`

**Response:**
```json
{
  "success": true,
  "message": "Contact blocked successfully"
}
```

---

## 4. Conversation Management APIs

### 4.1 Get Conversations
**GET** `/api/v1/conversations?limit=20&offset=0`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "id": 1,
        "type": "INDIVIDUAL",
        "name": "Jane Doe",
        "profileImageUrl": "https://cdn.example.com/profiles/jane.jpg",
        "lastMessage": {
          "id": 100,
          "content": "Hey, how are you?",
          "type": "TEXT",
          "timestamp": "2024-01-15T10:25:00Z",
          "sender": {
            "id": 2,
            "displayName": "Jane Doe"
          },
          "status": "READ"
        },
        "unreadCount": 0,
        "lastMessageAt": "2024-01-15T10:25:00Z",
        "participants": [
          {
            "id": 1,
            "user": {
              "id": 1,
              "displayName": "John Doe"
            },
            "role": "MEMBER"
          },
          {
            "id": 2,
            "user": {
              "id": 2,
              "displayName": "Jane Doe"
            },
            "role": "MEMBER"
          }
        ]
      }
    ],
    "pagination": {
      "total": 1,
      "limit": 20,
      "offset": 0,
      "hasMore": false
    }
  }
}
```

### 4.2 Create Conversation
**POST** `/api/v1/conversations`

**Request Body:**
```json
{
  "type": "GROUP",
  "name": "Family Group",
  "description": "Our family chat",
  "participantIds": [2, 3, 4],
  "groupImageUrl": "https://cdn.example.com/groups/family.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "type": "GROUP",
    "name": "Family Group",
    "description": "Our family chat",
    "groupImageUrl": "https://cdn.example.com/groups/family.jpg",
    "createdBy": {
      "id": 1,
      "displayName": "John Doe"
    },
    "participants": [
      {
        "id": 1,
        "user": {
          "id": 1,
          "displayName": "John Doe"
        },
        "role": "OWNER",
        "joinedAt": "2024-01-15T10:30:00Z"
      }
    ],
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 4.3 Get Conversation Details
**GET** `/api/v1/conversations/{conversationId}`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "GROUP",
    "name": "Family Group",
    "description": "Our family chat",
    "profileImageUrl": "https://cdn.example.com/groups/family.jpg",
    "createdBy": {
      "id": 1,
      "displayName": "John Doe"
    },
    "createdAt": "2024-01-15T10:30:00Z",
    "participantCount": 5,
    "settings": {
      "onlyAdminsCanSend": false,
      "onlyAdminsCanEditInfo": false,
      "disappearingMessages": null,
      "isMuted": false,
      "muteUntil": null
    },
    "participants": [
      {
        "id": 1,
        "user": {
          "id": 1,
          "displayName": "John Doe",
          "profilePictureUrl": "https://cdn.example.com/profiles/john.jpg"
        },
        "role": "OWNER",
        "joinedAt": "2024-01-15T10:30:00Z"
      }
    ]
  }
}
```

### 4.4 Update Conversation Settings
**PUT** `/api/v1/conversations/{conversationId}/settings`

**Request Body:**
```json
{
  "name": "Updated Group Name",
  "description": "New description",
  "onlyAdminsCanSend": true,
  "disappearingMessages": 86400
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Updated Group Name",
    "description": "New description",
    "settings": {
      "onlyAdminsCanSend": true,
      "disappearingMessages": 86400
    },
    "updatedAt": "2024-01-15T10:40:00Z"
  }
}
```

### 4.5 Add Participants
**POST** `/api/v1/conversations/{conversationId}/participants`

**Request Body:**
```json
{
  "participantIds": [5, 6]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "addedParticipants": [
      {
        "id": 5,
        "user": {
          "id": 5,
          "displayName": "Alice Johnson"
        },
        "role": "MEMBER",
        "joinedAt": "2024-01-15T10:35:00Z"
      }
    ]
  }
}
```

### 4.6 Remove Participant
**DELETE** `/api/v1/conversations/{conversationId}/participants/{participantId}`

**Response:**
```json
{
  "success": true,
  "message": "Participant removed successfully"
}
```

### 4.7 Leave Conversation
**POST** `/api/v1/conversations/{conversationId}/leave`

**Response:**
```json
{
  "success": true,
  "message": "Left conversation successfully"
}
```

### 4.8 Mute/Unmute Conversation
**POST** `/api/v1/conversations/{conversationId}/mute`

**Request Body:**
```json
{
  "duration": 3600
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isMuted": true,
    "muteUntil": "2024-01-15T11:40:00Z"
  }
}
```

---

## 5. Message Management APIs

### 5.1 Get Messages
**GET** `/api/v1/conversations/{conversationId}/messages?limit=50&offset=0&before=2024-01-15T10:30:00Z`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": 100,
        "conversationId": 1,
        "sender": {
          "id": 2,
          "displayName": "Jane Doe",
          "profilePictureUrl": "https://cdn.example.com/profiles/jane.jpg"
        },
        "type": "TEXT",
        "content": "Hey, how are you?",
        "timestamp": "2024-01-15T10:25:00Z",
        "status": "READ",
        "isEdited": false,
        "isDeleted": false,
        "isStarred": false,
        "replyToMessage": null,
        "forwardedFromMessage": null,
        "attachments": [],
        "reactions": [
          {
            "emoji": "👍",
            "users": [
              {
                "id": 1,
                "displayName": "John Doe"
              }
            ],
            "count": 1
          }
        ],
        "readBy": [
          {
            "userId": 1,
            "readAt": "2024-01-15T10:26:00Z"
          }
        ]
      }
    ],
    "pagination": {
      "total": 1,
      "limit": 50,
      "offset": 0,
      "hasMore": false
    }
  }
}
```

### 5.2 Send Message
**POST** `/api/v1/conversations/{conversationId}/messages`

**Request Body:**
```json
{
  "type": "TEXT",
  "content": "Hello everyone!",
  "replyToMessageId": null,
  "forwardedFromMessageIds": [],
  "attachments": []
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "conversationId": 1,
    "sender": {
      "id": 1,
      "displayName": "John Doe"
    },
    "type": "TEXT",
    "content": "Hello everyone!",
    "timestamp": "2024-01-15T10:30:00Z",
    "status": "SENT",
    "isEdited": false,
    "isDeleted": false,
    "attachments": [],
    "reactions": []
  }
}
```

### 5.3 Send Message with Attachment
**POST** `/api/v1/conversations/{conversationId}/messages`

**Request Body:**
```json
{
  "type": "IMAGE",
  "content": "Check out this photo!",
  "attachments": [
    {
      "type": "IMAGE",
      "fileName": "vacation.jpg",
      "fileUrl": "https://cdn.example.com/attachments/vacation.jpg",
      "thumbnailUrl": "https://cdn.example.com/thumbnails/vacation_thumb.jpg",
      "fileSize": 2048576,
      "mimeType": "image/jpeg",
      "width": 1920,
      "height": 1080
    }
  ]
}
```

### 5.4 Edit Message
**PUT** `/api/v1/messages/{messageId}`

**Request Body:**
```json
{
  "content": "Hello everyone! (edited)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "content": "Hello everyone! (edited)",
    "isEdited": true,
    "editedAt": "2024-01-15T10:32:00Z"
  }
}
```

### 5.5 Add Reaction
**POST** `/api/v1/messages/{messageId}/reactions`

**Request Body:**
```json
{
  "emoji": "❤️"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "messageId": 101,
    "emoji": "❤️",
    "user": {
      "id": 1,
      "displayName": "John Doe"
    },
    "createdAt": "2024-01-15T10:33:00Z"
  }
}
```

---

## 6. Call Management APIs

### 6.1 Initiate Call
**POST** `/api/v1/calls`

**Request Body:**
```json
{
  "conversationId": 1,
  "type": "VIDEO",
  "participantIds": [2]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "conversationId": 1,
    "type": "VIDEO",
    "status": "INITIATED",
    "initiator": {
      "id": 1,
      "displayName": "John Doe"
    },
    "participants": [
      {
        "id": 1,
        "user": {
          "id": 1,
          "displayName": "John Doe"
        },
        "status": "JOINED"
      },
      {
        "id": 2,
        "user": {
          "id": 2,
          "displayName": "Jane Doe"
        },
        "status": "INVITED"
      }
    ],
    "startedAt": "2024-01-15T10:35:00Z",
    "metadata": {
      "webrtcOffer": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n..."
    }
  }
}
```

### 6.2 Answer Call
**POST** `/api/v1/calls/{callId}/answer`

**Request Body:**
```json
{
  "webrtcAnswer": "v=0\r\no=- 987654321 2 IN IP4 127.0.0.1\r\n..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "ANSWERED",
    "answeredAt": "2024-01-15T10:35:30Z"
  }
}
```

### 6.3 Get Call History
**GET** `/api/v1/calls/history?limit=20&offset=0`

**Response:**
```json
{
  "success": true,
  "data": {
    "calls": [
      {
        "id": 1,
        "conversation": {
          "id": 1,
          "name": "Jane Doe"
        },
        "type": "VIDEO",
        "status": "ENDED",
        "initiator": {
          "id": 1,
          "displayName": "John Doe"
        },
        "duration": 300,
        "startedAt": "2024-01-15T10:35:00Z",
        "endedAt": "2024-01-15T10:40:00Z"
      }
    ],
    "pagination": {
      "total": 1,
      "limit": 20,
      "offset": 0,
      "hasMore": false
    }
  }
}
```

---

## 7. Story Management APIs

### 7.1 Get Stories Feed
**GET** `/api/v1/stories/feed?limit=20&offset=0`

**Response:**
```json
{
  "success": true,
  "data": {
    "stories": [
      {
        "user": {
          "id": 2,
          "displayName": "Jane Doe",
          "profilePictureUrl": "https://cdn.example.com/profiles/jane.jpg"
        },
        "stories": [
          {
            "id": 1,
            "type": "IMAGE",
            "mediaUrl": "https://cdn.example.com/stories/story1.jpg",
            "thumbnailUrl": "https://cdn.example.com/stories/story1_thumb.jpg",
            "content": "Beautiful sunset!",
            "createdAt": "2024-01-15T09:00:00Z",
            "expiresAt": "2024-01-16T09:00:00Z",
            "viewCount": 5,
            "isViewed": false
          }
        ]
      }
    ]
  }
}
```

### 7.2 Post Story
**POST** `/api/v1/stories`

**Request Body:**
```json
{
  "type": "IMAGE",
  "content": "Amazing day at the beach!",
  "mediaUrl": "https://cdn.example.com/stories/beach.jpg",
  "thumbnailUrl": "https://cdn.example.com/stories/beach_thumb.jpg",
  "privacy": "CONTACTS"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "type": "IMAGE",
    "content": "Amazing day at the beach!",
    "mediaUrl": "https://cdn.example.com/stories/beach.jpg",
    "thumbnailUrl": "https://cdn.example.com/stories/beach_thumb.jpg",
    "privacy": "CONTACTS",
    "createdAt": "2024-01-15T10:40:00Z",
    "expiresAt": "2024-01-16T10:40:00Z"
  }
}
```

---

## 8. Group Admin APIs

### 8.1 Promote to Admin
**POST** `/api/v1/conversations/{conversationId}/admin/promote`

**Request Body:**
```json
{
  "participantId": 3,
  "permissions": {
    "canAddMembers": true,
    "canRemoveMembers": false,
    "canEditGroupInfo": true,
    "canSendMessages": true
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "participantId": 3,
    "role": "ADMIN",
    "permissions": {
      "canAddMembers": true,
      "canRemoveMembers": false,
      "canEditGroupInfo": true,
      "canSendMessages": true
    },
    "promotedAt": "2024-01-15T10:45:00Z"
  }
}
```

### 8.2 Get Admin Actions
**GET** `/api/v1/conversations/{conversationId}/admin/actions?limit=20&offset=0`

**Response:**
```json
{
  "success": true,
  "data": {
    "actions": [
      {
        "id": 1,
        "adminUser": {
          "id": 1,
          "displayName": "John Doe"
        },
        "targetUser": {
          "id": 3,
          "displayName": "Bob Wilson"
        },
        "actionType": "PROMOTE_TO_ADMIN",
        "reason": "Trusted member",
        "createdAt": "2024-01-15T10:45:00Z"
      }
    ]
  }
}
```

---

## 9. Media Upload APIs

### 9.1 Upload Media
**POST** `/api/v1/media/upload`

**Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "Content-Type": "multipart/form-data"
}
```

**Request Body (Form Data):**
```
file: [binary file data]
type: "image"
conversationId: "1"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "fileId": "file_abc123",
    "fileName": "image.jpg",
    "fileUrl": "https://cdn.example.com/attachments/image.jpg",
    "thumbnailUrl": "https://cdn.example.com/thumbnails/image_thumb.jpg",
    "fileSize": 1024576,
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "uploadedAt": "2024-01-15T10:50:00Z"
  }
}
```

---

## 10. WebSocket/STOMP Real-time APIs

### 10.1 WebSocket Connection
**Connection URL:** `wss://api.whatsapp.com/ws/stomp`

**Connection Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "X-Session-Id": "sess_abc123def456"
}
```

### 10.2 STOMP Message Destinations

#### Send Message
**Destination:** `/app/chat.message`
**Message:**
```json
{
  "conversationId": 1,
  "type": "TEXT",
  "content": "Hello from WebSocket!",
  "timestamp": "2024-01-15T10:55:00Z"
}
```

#### Typing Indicator
**Destination:** `/app/chat.typing`
**Message:**
```json
{
  "conversationId": 1,
  "userId": 1,
  "userName": "John Doe",
  "isTyping": true
}
```

#### Message Status Update
**Destination:** `/app/chat.status`
**Message:**
```json
{
  "messageId": 101,
  "userId": 1,
  "status": "READ",
  "timestamp": "2024-01-15T10:56:00Z"
}
```

#### User Presence Update
**Destination:** `/app/presence.update`
**Message:**
```json
{
  "userId": 1,
  "isOnline": true,
  "lastSeenAt": "2024-01-15T10:57:00Z"
}
```

#### Call Signaling
**Destination:** `/app/call.signal`
**Message:**
```json
{
  "callId": 1,
  "type": "webrtc-offer",
  "data": {
    "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n...",
    "type": "offer"
  }
}
```

### 10.3 STOMP Subscription Queues

#### Personal Message Queue
**Queue:** `/queue/messages/{userId}`
**Received Message:**
```json
{
  "type": "NEW_MESSAGE",
  "data": {
    "id": 102,
    "conversationId": 1,
    "sender": {
      "id": 2,
      "displayName": "Jane Doe"
    },
    "content": "Hi there!",
    "timestamp": "2024-01-15T11:00:00Z"
  }
}
```

#### Personal Call Queue
**Queue:** `/queue/calls/{userId}`
**Received Message:**
```json
{
  "type": "INCOMING_CALL",
  "data": {
    "callId": 2,
    "conversationId": 1,
    "initiator": {
      "id": 2,
      "displayName": "Jane Doe"
    },
    "type": "VIDEO",
    "webrtcOffer": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n..."
  }
}
```

#### Personal Presence Queue
**Queue:** `/queue/presence/{userId}`
**Received Message:**
```json
{
  "type": "USER_ONLINE",
  "data": {
    "userId": 2,
    "displayName": "Jane Doe",
    "isOnline": true,
    "lastSeenAt": "2024-01-15T11:01:00Z"
  }
}
```

#### Personal Typing Queue
**Queue:** `/queue/typing/{userId}`
**Received Message:**
```json
{
  "type": "TYPING_INDICATOR",
  "data": {
    "conversationId": 1,
    "userId": 2,
    "userName": "Jane Doe",
    "isTyping": true
  }
}
```

#### Personal Notifications Queue
**Queue:** `/queue/notifications/{userId}`
**Received Message:**
```json
{
  "type": "MESSAGE_REACTION",
  "data": {
    "messageId": 101,
    "emoji": "❤️",
    "user": {
      "id": 2,
      "displayName": "Jane Doe"
    }
  }
}
```

---

## 11. Error Responses

### Standard Error Format
```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "field": "email",
    "rejectedValue": "invalid-email",
    "message": "Invalid email format"
  },
  "statusCode": 400,
  "timestamp": "2024-01-15T11:05:00Z"
}
```

### Common Error Codes
- `AUTH_001`: Invalid credentials
- `AUTH_002`: Token expired
- `AUTH_003`: OTP invalid or expired
- `VALIDATION_001`: Required field missing
- `VALIDATION_002`: Invalid format
- `PERMISSION_001`: Insufficient permissions
- `RESOURCE_001`: Resource not found
- `RATE_LIMIT_001`: Too many requests

---

## 12. Rate Limiting

### Rate Limit Headers
```json
{
  "X-RateLimit-Limit": "100",
  "X-RateLimit-Remaining": "95",
  "X-RateLimit-Reset": "1642248000"
}
```

### Rate Limits by Endpoint
- **Authentication**: 5 requests/minute
- **Messages**: 100 requests/minute
- **File Upload**: 10 requests/minute
- **Search**: 20 requests/minute
- **General APIs**: 1000 requests/hour

---

## 13. Pagination

### Standard Pagination Parameters
- `limit`: Number of items per page (default: 20, max: 100)
- `offset`: Number of items to skip (default: 0)
- `before`: Timestamp for cursor-based pagination (messages)
- `after`: Timestamp for cursor-based pagination (messages)

### Pagination Response Format
```json
{
  "pagination": {
    "total": 150,
    "limit": 20,
    "offset": 0,
    "hasMore": true,
    "nextOffset": 20,
    "prevOffset": null
  }
}
```

This comprehensive API documentation covers all major WhatsApp-like system functionalities with complete request/response examples, WebSocket/STOMP integration, and proper error handling patterns.