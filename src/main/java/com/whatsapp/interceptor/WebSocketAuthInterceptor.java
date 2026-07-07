package com.whatsapp.interceptor;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.whatsapp.util.EncryptionUtil;
import com.whatsapp.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

	private final JwtUtil jwtUtil;

	@Value("${app.encryption.secret:}")
	private String encryptionSecret;

	@Value("${app.encryption.enabled:false}")
	private boolean encryptionEnabled;

	@Override
	public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
			@NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {

		try {
			String token = extractToken(request);

			if (token == null) {
				log.warn("WebSocket handshake failed - no token from: {}", request.getRemoteAddress());
				return false;
			}

			// Decrypt token if encryption is enabled
			if (encryptionEnabled && StringUtils.hasText(encryptionSecret)) {
				try {
					token = EncryptionUtil.decrypt(token, encryptionSecret);
				} catch (Exception e) {
					log.warn("WebSocket token decryption failed, trying as plain token: {}", e.getMessage());
				}
			}

			if (!jwtUtil.validateToken(token)) {
				log.warn("WebSocket handshake failed - invalid token from: {}", request.getRemoteAddress());
				return false;
			}

			Long userId = jwtUtil.getUserIdFromToken(token);
			String sessionId = jwtUtil.getSessionIdFromToken(token);

			attributes.put("userId", userId);
			attributes.put("sessionId", sessionId);

			log.info("WebSocket handshake successful - userId: {}", userId);
			return true;

		} catch (Exception e) {
			log.error("WebSocket handshake error from {}: {}", request.getRemoteAddress(), e.getMessage());
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
		// Try query parameter first (for SockJS)
		String query = request.getURI().getQuery();
		if (query != null) {
			String[] params = query.split("&");
			for (String param : params) {
				if (param.startsWith("token=")) {
					String token = param.substring(6);
					log.debug("Token found in query parameter");
					return token;
				}
				if (param.startsWith("access_token=")) {
					String token = param.substring(13);
					log.debug("Token found in access_token query parameter");
					return token;
				}
			}
		}

		// Try Authorization header
		String authHeader = request.getHeaders().getFirst("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			log.debug("Token found in Authorization header");
			return authHeader.substring(7);
		}

		// Try Sec-WebSocket-Protocol header (some clients send token here)
		String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
		if (protocol != null && protocol.contains(",")) {
			String[] protocols = protocol.split(",");
			for (String p : protocols) {
				p = p.trim();
				if (p.startsWith("Bearer-")) {
					log.debug("Token found in Sec-WebSocket-Protocol header");
					return p.substring(7);
				}
			}
		}

		log.warn("No token found in request");
		return null;
	}
}
