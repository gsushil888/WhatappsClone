package com.whatsapp.filter;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.MDC;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.whatsapp.entity.UserSession;
import com.whatsapp.repository.UserSessionRepository;
import com.whatsapp.util.JwtUtil;
import com.whatsapp.util.RequestContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserSessionRepository userSessionRepository;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String requestUri = request.getRequestURI();
    String method = request.getMethod();
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      try {
        String token = authHeader.substring(7);

        if (jwtUtil.validateToken(token)) {
          Long userId = jwtUtil.getUserIdFromToken(token);
          String sessionId = jwtUtil.getSessionIdFromToken(token);

          boolean isSessionActive = userSessionRepository.findById(sessionId)
              .map(session -> session.getStatus() == UserSession.SessionStatus.ACTIVE)
              .orElse(false);

          if (isSessionActive) {
            RequestContext.RequestInfo info = RequestContext.getContext();
            if (info != null) {
              info.setUserId(userId);
              info.setSessionId(sessionId);
              MDC.put("userId", String.valueOf(userId));
              MDC.put("sessionId", sessionId);
              response.setHeader("X-Correlation-ID", info.getCorrelationId());
            }

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContextHolder.getContext().setAuthentication(authentication);
          } else {
            log.warn("[JWT-FILTER] Session inactive - userId: {}, sessionId: {}, path: {} {}",
                userId, sessionId, method, requestUri);
          }
        } else {
          log.warn("[JWT-FILTER] Invalid token - path: {} {}", method, requestUri);
        }

      } catch (Exception ex) {
        log.error("[JWT-FILTER] Authentication failed - path: {} {}, error: {}", method, requestUri,
            ex.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }
}
