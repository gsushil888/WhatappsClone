package com.whatsapp.listener;

import com.whatsapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
		StompHeaderAccessor headerAccessor = StompHeaderAccessor
				.wrap(event.getMessage());

		var sessionAttributes = headerAccessor.getSessionAttributes();
		if (sessionAttributes != null) {
			Object userIdObj = sessionAttributes.get("userId");
			String deviceInfo = headerAccessor
					.getFirstNativeHeader("Device-Info");

			if (userIdObj != null) {
				String userId = userIdObj.toString();
				Long userIdLong = Long.parseLong(userId);
				presenceService.setUserOnline(userIdLong, deviceInfo);
				log.info("User {} connected via WebSocket", userId);
			}
		} else {
			var user = headerAccessor.getUser();
			if (user != null) {
				String userId = user.getName();
				if (userId != null) {
					Long userIdLong = Long.parseLong(userId);
					presenceService.setUserOnline(userIdLong, null);
					log.info("User {} connected via WebSocket", userId);
				}
			}
		}
	}

	@EventListener
	public void handleWebSocketDisconnectListener(
			SessionDisconnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor
				.wrap(event.getMessage());

		String userId = null;
		var sessionAttributes = headerAccessor.getSessionAttributes();
		if (sessionAttributes != null) {
			Object userIdObj = sessionAttributes.get("userId");
			if (userIdObj != null) {
				userId = userIdObj.toString();
			}
		} else {
			var user = headerAccessor.getUser();
			if (user != null) {
				userId = user.getName();
			}
		}

		if (userId != null) {
			Long userIdLong = Long.parseLong(userId);
			presenceService.setUserOffline(userIdLong);
			log.info("User {} disconnected from WebSocket - set to OFFLINE",
					userId);
		} else {
			log.warn(
					"WebSocket disconnect event but no userId found in session");
		}
	}
}
