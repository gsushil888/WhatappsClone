package com.whatsapp.interceptor;

import com.whatsapp.util.JwtUtil;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

	private final JwtUtil jwtUtil;

	@Override
	public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
			@NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {

		String correlationId = RequestContext.generateCorrelationId();

		try {
			String token = extractToken(request);

			if (token == null) {
				log.warn("[{}] WebSocket handshake failed: No token provided", correlationId);
				return false;
			}

			if (!jwtUtil.validateToken(token)) {
				log.warn("[{}] WebSocket handshake failed: Invalid token", correlationId);
				return false;
			}

			Long userId = jwtUtil.getUserIdFromToken(token);
			String sessionId = jwtUtil.getSessionIdFromToken(token);
			String deviceId = jwtUtil.getDeviceIdFromToken(token);

			attributes.put("userId", userId);
			attributes.put("sessionId", sessionId);
			attributes.put("deviceId", deviceId);
			attributes.put("correlationId", correlationId);

			log.info("[{}] WebSocket handshake successful for user: {}", correlationId, userId);
			return true;

		} catch (Exception e) {
			log.error("[{}] WebSocket handshake error: {}", correlationId, e.getMessage());
			return false;
		}
	}

	@Override
	public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
			@NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

		if (exception != null) {
			log.error("WebSocket handshake completed with error: {}", exception.getMessage());
		} else {
			log.debug("WebSocket handshake completed successfully");
		}
	}

	private String extractToken(ServerHttpRequest request) {
		String query = request.getURI().getQuery();
		if (query != null) {
			String[] params = query.split("&");
			for (String param : params) {
				if (param.startsWith("token=")) {
					return param.substring(6);
				}
			}
		}

		String authHeader = request.getHeaders().getFirst("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}

		return null;
	}
}