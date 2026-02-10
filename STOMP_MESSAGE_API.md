# STOMP WebSocket Message API

## Send Message via WebSocket

### Endpoint
**STOMP SEND** `/app/chat.message`

### Request Headers
```
Authorization: Bearer <jwt_token>
conversationId: <conversation_id>
```

### Request Body
```json
{
  "messageType": "TEXT",
  "content": "Hello World!",
  "mediaUrl": null,
  "mediaMetadata": null,
  "location": null,
  "replyToMessageId": null
}
```

### Response (Broadcast to `/topic/conversation/{conversationId}`)
```json
{
  "id": 123,
  "senderId": 1,
  "senderName": "John Doe",
  "senderAvatar": "https://...",
  "content": "Hello World!",
  "messageType": "TEXT",
  "mediaUrl": null,
  "mediaMetadata": null,
  "replyToMessageId": null,
  "isEdited": false,
  "editedAt": null,
  "reactions": [],
  "deliveryStatus": {
    "sent": 1,
    "delivered": 0,
    "read": 0
  },
  "createdAt": "2024-02-10T10:30:00"
}
```

### Error Response (Sent to `/user/queue/errors`)
```json
{
  "error": "Error message description"
}
```

## Client Connection Example

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: 'Bearer ' + token },
  function(frame) {
    // Subscribe to conversation messages
    stompClient.subscribe('/topic/conversation/' + conversationId, function(message) {
      const response = JSON.parse(message.body);
      console.log('New message:', response);
    });

    // Subscribe to errors
    stompClient.subscribe('/user/queue/errors', function(error) {
      console.error('Error:', JSON.parse(error.body));
    });

    // Send message
    stompClient.send('/app/chat.message', 
      { conversationId: conversationId },
      JSON.stringify({
        messageType: 'TEXT',
        content: 'Hello World!'
      })
    );
  }
);
```
