# WhatsApp Clone Backend

A comprehensive WhatsApp-like application backend built with Spring Boot, WebSocket, Redis, and MySQL.

## Features

### Authentication & Authorization

- ✅ Multi-factor authentication (Username+Password+OTP, Email+OTP, Mobile+OTP)
- ✅ JWT token-based authentication with refresh tokens
- ✅ Session management with device-based restrictions
- ✅ Redis caching for sessions and OTP
- ✅ Comprehensive error logging with correlation IDs
- ✅ Device fingerprinting and session validation

### Real-time Communication (WebSocket)

- ✅ Real-time messaging with delivery and read receipts
- ✅ Typing indicators
- ✅ Online/offline presence tracking
- ✅ Audio and video calling with WebRTC signaling
- ✅ Group chat support
- ✅ File sharing (images, videos, documents, audio)
- ✅ Message forwarding and replies

### User Management

- ✅ Profile management (image, status, settings)
- ✅ Contact management
- ✅ Email and mobile number updates with OTP verification
- ✅ Session management and termination
- ✅ Notification settings

### Chat Features

- ✅ Individual and group chats
- ✅ Message status tracking (sent, delivered, read)
- ✅ Unseen message counts
- ✅ Message search
- ✅ File attachments with type validation

## Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Authentication**: JWT with Spring Security
- **Real-time**: WebSocket with STOMP
- **File Upload**: Multipart with validation
- **Email**: Spring Mail
- **Build Tool**: Maven

## API Endpoints

### Authentication

```
POST /api/v1/auth/login              # Universal login (email/mobile/username)
POST /api/v1/auth/otp/verify         # Verify OTP and complete login
POST /api/v1/auth/refresh            # Refresh access token
POST /api/v1/auth/logout             # Logout and invalidate session
POST /api/v1/auth/register           # Register new user
```

### User Management

```
GET    /api/v1/users/me             # Get user profile
PUT    /api/v1/users/me             # Update profile
PUT    /api/v1/users/me/privacy     # Update privacy settings
GET    /api/v1/users/search         # Search users
GET    /api/v1/users/me/sessions    # Get active sessions
DELETE /api/v1/users/me/sessions/{id} # Terminate session
```

### Contacts

```
GET    /api/v1/contacts             # Get user contacts
POST   /api/v1/contacts             # Add new contact
PUT    /api/v1/contacts/{id}        # Update contact
POST   /api/v1/contacts/{id}/block  # Block contact
POST   /api/v1/contacts/{id}/unblock # Unblock contact
GET    /api/v1/contacts/search      # Search contacts
POST   /api/v1/contacts/sync        # Sync contacts
```

### Conversations

```
GET    /api/v1/conversations        # Get chat list
POST   /api/v1/conversations        # Create conversation
GET    /api/v1/conversations/{id}   # Get conversation details
PUT    /api/v1/conversations/{id}/settings # Update settings
POST   /api/v1/conversations/{id}/participants # Add participants
DELETE /api/v1/conversations/{id}/participants/{pid} # Remove participant
POST   /api/v1/conversations/{id}/leave # Leave group
```

### Messages

```
GET    /api/v1/conversations/{id}/messages # Get messages
POST   /api/v1/conversations/{id}/messages # Send message
PUT    /api/v1/messages/{id}        # Edit message
DELETE /api/v1/messages/{id}        # Delete message
POST   /api/v1/messages/{id}/star   # Star message
DELETE /api/v1/messages/{id}/star   # Unstar message
POST   /api/v1/messages/{id}/reactions # Add reaction
DELETE /api/v1/messages/{id}/reactions/{emoji} # Remove reaction
GET    /api/v1/conversations/{id}/messages/search # Search messages
GET    /api/v1/messages/starred     # Get starred messages
```

### Calls

```
POST   /api/v1/calls                # Initiate call
POST   /api/v1/calls/{id}/answer    # Answer call
POST   /api/v1/calls/{id}/decline   # Decline call
POST   /api/v1/calls/{id}/end       # End call
GET    /api/v1/calls/history        # Get call history
GET    /api/v1/calls/statistics     # Get call statistics
```

### Stories

```
GET    /api/v1/stories/feed         # Get story feed
POST   /api/v1/stories              # Post story
POST   /api/v1/stories/{id}/view    # View story
GET    /api/v1/stories/{id}/viewers # Get story viewers
DELETE /api/v1/stories/{id}         # Delete story
GET    /api/v1/stories/my-stories   # Get my stories
GET    /api/v1/stories/user/{id}    # Get user stories
```

### Media & Notifications

```
POST   /api/v1/media/upload         # Upload media
POST   /api/v1/notifications/register # Register device for push notifications
```

### WebSocket Endpoints

```
/ws                               # Main WebSocket endpoint
/ws/stomp                         # STOMP WebSocket endpoint
```

### STOMP Message Mappings

```
/app/chat.message                 # Send chat message
/app/chat.typing                  # Send typing indicator
/app/chat.status                  # Update message status
/app/chat.reaction                # Add/remove message reaction
/app/presence.update              # Update user presence
/app/call.initiate                # Initiate call
/app/call.signal                  # WebRTC signaling
/app/story.post                   # Post story
/app/story.view                   # View story
```

### Queue Endpoints

```
/queue/messages/{userId}          # Personal message queue
/queue/calls/{userId}             # Personal call notifications
/queue/notifications/{userId}     # Personal notifications
/queue/presence/{userId}          # Personal presence updates
/queue/stories/{userId}           # Personal story notifications
/queue/typing/{userId}            # Personal typing indicators
/queue/groups/{userId}            # Personal group updates
/queue/errors/{userId}            # Personal error notifications
```

## Configuration

### Application Properties

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/whatapps_db
    username: root
    password: root

  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379

# JWT Configuration
jwt:
  secret: your-secret-key
  access-token-validity: 900000 # 15 minutes
  refresh-token-validity: 86400000 # 24 hours

  # File Upload
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

  # Email Configuration
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

## Setup Instructions

### Prerequisites

- Java 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### Installation

1. Clone the repository

```bash
git clone <repository-url>
cd whatapps-backend-latest
```

2. Create MySQL database

```sql
CREATE DATABASE whatapps_db;
```

3. Update application properties with your database and email credentials

4. Start Redis server

```bash
redis-server
```

5. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## WebSocket Connection

### Authentication

WebSocket connections require authentication via headers:

```javascript
const headers = {
  Authorization: "Bearer " + accessToken,
  "Session-Id": sessionId,
};
```

### Message Format

```javascript
// Send message
{
    "chatId": 1,
    "content": "Hello World",
    "messageType": "TEXT",
    "fileUrl": null,
    "fileName": null,
    "fileSize": null,
    "replyToMessageId": null,
    "forwardedFromMessageIds": []
}

// Typing indicator
{
    "chatId": 1,
    "userId": 1,
    "userName": "John Doe",
    "isTyping": true
}

// Message status update
{
    "messageId": 1,
    "userId": 1,
    "status": "READ",
    "timestamp": "2024-01-01T12:00:00Z"
}
```

## Error Handling

The application uses comprehensive error handling with:

- Global exception handler
- Correlation ID tracking
- Database error logging
- Structured error responses

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "errorCode": "AUTH_001",
  "correlationId": "uuid",
  "data": null,
  "statusCode": 400
}
```

## Security Features

- JWT token validation
- Session-based authentication
- Device-based session management
- CORS configuration
- File type validation
- SQL injection prevention
- XSS protection

## Monitoring & Logging

- Structured logging with correlation IDs
- Error logging to database
- Performance monitoring
- Health check endpoints
- Metrics exposure

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package -Pprod
```

### Docker Support

```dockerfile
FROM openjdk:17-jre-slim
COPY target/whatapps-backend-latest-1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.
