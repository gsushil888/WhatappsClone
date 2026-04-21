package com.whatsapp.interceptor;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.whatsapp.util.JwtUtil;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

  private final JwtUtil jwtUtil;

  @Override
  public boolean beforeHandshake(@NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler,
      @NonNull Map<String, Object> attributes) throws Exception {

    String correlationId = RequestContext.generateCorrelationId();
    log.info("[{}] WebSocket handshake attempt from: {}", correlationId,
        request.getRemoteAddress());

    try {
      String token = extractToken(request);

      if (token == null) {
        log.warn("[{}] WebSocket handshake failed: No token provided", correlationId);
        log.warn("[{}] Request URI: {}", correlationId, request.getURI());
        log.warn("[{}] Headers: {}", correlationId, request.getHeaders());
        return false;
      }

      log.info("[{}] Token found, validating...", correlationId);

      if (!jwtUtil.validateToken(token)) {
        log.warn("[{}] WebSocket handshake failed: Invalid token", correlationId);
        return false;
      }

      Long userId = jwtUtil.getUserIdFromToken(token);
      String sessionId = jwtUtil.getSessionIdFromToken(token);

      attributes.put("userId", userId);
      attributes.put("sessionId", sessionId);
      attributes.put("correlationId", correlationId);

      log.info("[{}] WebSocket handshake successful for user: {}, sessionId: {}", correlationId,
          userId, sessionId);
      return true;

    } catch (Exception e) {
      log.error("[{}] WebSocket handshake error: {}", correlationId, e.getMessage(), e);
      return false;
    }
  }

  @Override
  public void afterHandshake(@NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler,
      @Nullable Exception exception) {

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
