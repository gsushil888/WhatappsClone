package com.whatsapp.filter;

import java.io.IOException;
import java.util.Collections;

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
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String requestUri = request.getRequestURI();
		String method = request.getMethod();
		String authHeader = request.getHeader("Authorization");

		log.info("[JWT-FILTER] {} {} - Auth header present: {}", method, requestUri, authHeader != null);

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			try {
				String token = authHeader.substring(7);
				log.info("[JWT-FILTER] Validating token for request: {} {}", method, requestUri);

				if (jwtUtil.validateToken(token)) {
					Long userId = jwtUtil.getUserIdFromToken(token);
					String sessionId = jwtUtil.getSessionIdFromToken(token);

					log.info("[JWT-FILTER] Token valid - userId: {}, sessionId: {}", userId, sessionId);

					boolean isSessionActive = userSessionRepository.findById(sessionId)
							.map(session -> session.getStatus() == UserSession.SessionStatus.ACTIVE).orElse(false);

					if (isSessionActive) {
						RequestContext.RequestInfo info = RequestContext.getContext();
						if (info != null) {
							info.setUserId(userId);
							info.setSessionId(sessionId);
						}

						UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
								userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

						SecurityContextHolder.getContext().setAuthentication(authentication);
						log.info("[JWT-FILTER] Authentication successful - userId: {}, sessionId: {}, path: {} {}",
								userId, sessionId, method, requestUri);
					} else {
						log.warn("[JWT-FILTER] Session {} inactive for userId: {}, path: {} {}", sessionId, userId,
								method, requestUri);
					}
				} else {
					log.warn("[JWT-FILTER] Invalid token for request: {} {}", method, requestUri);
				}

			} catch (Exception ex) {
				log.error("[JWT-FILTER] Authentication failed for {} {}: {}", method, requestUri, ex.getMessage());
			}
		} else {
			log.info("[JWT-FILTER] No Bearer token found for: {} {}", method, requestUri);
		}

		filterChain.doFilter(request, response);
		log.info("[JWT-FILTER] Request completed: {} {}", method, requestUri);
	}
}
