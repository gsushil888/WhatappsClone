package com.whatsapp.filter;

import com.whatsapp.util.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
public class RequestContextFilter extends OncePerRequestFilter {

	private static final String[] DEVICE_HEADERS = { "X-Device-Type", "X-Device-OS", "X-Device-OS-Version",
			"X-App-Version", "X-Device-Model", "X-Device-Fingerprint" };

	private static final String[] TRACKED_HEADERS = { "Authorization", "User-Agent", "X-Forwarded-For", "X-Real-IP",
			"X-Device-Fingerprint" };

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain chain) throws ServletException, IOException {

		String requestUri = request.getRequestURI();
		String method = request.getMethod();
		String correlationId = getOrGenerateCorrelationId(request);

		log.info("[REQUEST-START] {} {} - correlationId: {}", method, requestUri, correlationId);

		RequestContext.RequestInfo requestInfo = RequestContext.RequestInfo.builder().correlationId(correlationId)
				.deviceInfo(extractDeviceInfo(request)).headers(extractHeaders(request))
				.ipAddress(extractClientIpAddress(request)).userAgent(request.getHeader("User-Agent"))
				.startTime(System.currentTimeMillis()).build();

		RequestContext.setContext(requestInfo);

		response.setHeader("X-Correlation-ID", correlationId);
		response.setHeader("X-Request-ID", correlationId);

		MDC.put("correlationId", correlationId);
		MDC.put("ipAddress", requestInfo.getIpAddress());
		MDC.put("method", method);
		MDC.put("uri", requestUri);

		log.info("[REQUEST-CONTEXT] IP: {}, UserAgent: {}, Device: {}", requestInfo.getIpAddress(),
				requestInfo.getUserAgent(), requestInfo.getDeviceInfo());

		try {
			chain.doFilter(request, response);

			long duration = System.currentTimeMillis() - requestInfo.getStartTime();
			log.info("[REQUEST-END] {} {} - Status: {}, Duration: {}ms, correlationId: {}", method, requestUri,
					response.getStatus(), duration, correlationId);

		} catch (Exception e) {
			long duration = System.currentTimeMillis() - requestInfo.getStartTime();
			log.error("[REQUEST-ERROR] {} {} - Duration: {}ms, correlationId: {}, Error: {}", method, requestUri,
					duration, correlationId, e.getMessage(), e);
			throw e;
		} finally {
			RequestContext.clear();
			MDC.clear();
		}
	}

	private String getOrGenerateCorrelationId(HttpServletRequest request) {
		String id = request.getHeader("X-Correlation-ID");
		if (id == null || id.isBlank()) {
			id = request.getHeader("X-Request-ID");
		}
		return id != null ? id : RequestContext.generateCorrelationId();
	}

	private Map<String, String> extractDeviceInfo(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();
		for (String header : DEVICE_HEADERS) {
			String value = request.getHeader(header);
			if (value != null && !value.isBlank()) {
				String key = header.toLowerCase().replace("x-", "").replace("-", "_");
				map.put(key, value);
			}
		}
		return map.isEmpty() ? Collections.emptyMap() : map;
	}

	private Map<String, String> extractHeaders(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();
		for (String header : TRACKED_HEADERS) {
			String value = request.getHeader(header);
			if (value != null && !value.isBlank()) {
				String key = header.toLowerCase().replace("-", "_");
				map.put(key, value);
			}
		}
		return map;
	}

	private String extractClientIpAddress(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
				.filter(ip -> !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)).map(ip -> ip.split(",")[0].trim())
				.orElseGet(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
						.filter(ip -> !ip.isBlank() && !"unknown".equalsIgnoreCase(ip))
						.orElse(request.getRemoteAddr()));
	}
}
