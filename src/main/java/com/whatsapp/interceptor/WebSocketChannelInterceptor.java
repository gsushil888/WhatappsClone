package com.whatsapp.interceptor;

import java.util.Collections;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.whatsapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ExecutorChannelInterceptor {

	private final JwtUtil jwtUtil;

	@Override
	public Message<?> preSend(@NonNull Message<?> message,
			@NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor
				.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null
				|| accessor.getCommand() == StompCommand.DISCONNECT) {
			return message;
		}

		var sessionAttributes = accessor.getSessionAttributes();
		String userIdStr = null;
		String sessionIdStr = null;

		if (sessionAttributes != null) {
			Object uid = sessionAttributes.get("userId");
			Object sid = sessionAttributes.get("sessionId");
			if (uid != null)
				userIdStr = uid.toString();
			if (sid != null)
				sessionIdStr = sid.toString();
		}

		if (userIdStr == null
				&& accessor.getCommand() == StompCommand.CONNECT) {
			String authHeader = accessor.getFirstNativeHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				try {
					String token = authHeader.substring(7);
					if (jwtUtil.validateToken(token)) {
						userIdStr = String
								.valueOf(jwtUtil.getUserIdFromToken(token));
						sessionIdStr = jwtUtil.getSessionIdFromToken(token);
						if (sessionAttributes != null) {
							sessionAttributes.put("userId",
									Long.parseLong(userIdStr));
							sessionAttributes.put("sessionId", sessionIdStr);
						}
					}
				} catch (Exception e) {
					log.warn("Failed to extract userId from CONNECT token: {}",
							e.getMessage());
				}
			}
		}

		if (userIdStr != null) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userIdStr, null, Collections.singletonList(
							new SimpleGrantedAuthority("ROLE_USER")));
			accessor.setUser(authentication);
			SecurityContextHolder.getContext()
					.setAuthentication(authentication);
		} else {
			log.warn("No userId found for WebSocket command: {}",
					accessor.getCommand());
		}

		return message;
	}

	// Runs on the actual handler thread — correct place to set MDC
	public Message<?> beforeHandle(@NonNull Message<?> message,
			@NonNull MessageChannel channel,
			@NonNull java.util.concurrent.Executor executor) {
		StompHeaderAccessor accessor = MessageHeaderAccessor
				.getAccessor(message, StompHeaderAccessor.class);
		if (accessor != null) {
			var sessionAttributes = accessor.getSessionAttributes();
			if (sessionAttributes != null) {
				Object uid = sessionAttributes.get("userId");
				Object sid = sessionAttributes.get("sessionId");
				if (uid != null) {
					MDC.put("userId", uid.toString());
					MDC.put("sessionId", sid != null ? sid.toString() : "");
					MDC.put("correlationId", "req_" + UUID.randomUUID()
							.toString().replace("-", "").substring(0, 12));
				}
			}
		}
		return message;
	}

	@Override
	public void afterMessageHandled(@NonNull Message<?> message,
			@NonNull MessageChannel channel,
			@NonNull org.springframework.messaging.MessageHandler handler,
			Exception ex) {
		MDC.clear();
	}
}
