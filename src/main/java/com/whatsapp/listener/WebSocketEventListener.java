package com.whatsapp.listener;

import com.whatsapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

	private final PresenceService presenceService;

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		var sessionAttributes = headerAccessor.getSessionAttributes();

		Long userId = extractUserId(headerAccessor, sessionAttributes);
		if (userId != null) {
			setMdc(userId, sessionAttributes);
			String deviceInfo = headerAccessor.getFirstNativeHeader("Device-Info");
			presenceService.setUserOnline(userId, deviceInfo);
			log.info("User {} connected via WebSocket", userId);
			MDC.clear();
		}
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		var sessionAttributes = headerAccessor.getSessionAttributes();

		Long userId = extractUserId(headerAccessor, sessionAttributes);
		if (userId != null) {
			setMdc(userId, sessionAttributes);
			presenceService.setUserOffline(userId);
			log.info("User {} disconnected from WebSocket", userId);
			MDC.clear();
		} else {
			log.warn("WebSocket disconnect - no userId found in session");
		}
	}

	private void setMdc(Long userId, java.util.Map<String, Object> sessionAttributes) {
		MDC.put("userId", String.valueOf(userId));
		if (sessionAttributes != null) {
			Object sessionId = sessionAttributes.get("sessionId");
			MDC.put("sessionId", sessionId != null ? sessionId.toString() : "");
		}
		MDC.put("correlationId", "req_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
	}

	private Long extractUserId(StompHeaderAccessor accessor, java.util.Map<String, Object> sessionAttributes) {
		if (sessionAttributes != null) {
			Object userIdObj = sessionAttributes.get("userId");
			if (userIdObj != null) return Long.parseLong(userIdObj.toString());
		}
		var user = accessor.getUser();
		if (user != null && user.getName() != null) return Long.parseLong(user.getName());
		return null;
	}
}
