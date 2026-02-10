package com.whatsapp.aspect;

import java.util.Arrays;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.whatsapp.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

	@Around("execution(* com.whatsapp.controller..*(..))")
	public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
		String correlationId = RequestContext.getCorrelationId();
		String methodName = joinPoint.getSignature().getName();
		String className = joinPoint.getTarget().getClass().getSimpleName();

		Map<String, String> deviceInfo = RequestContext.getDeviceInfo();
		Map<String, String> headers = RequestContext.getHeaders();

		log.info("[{}] Controller: {}.{} - Args: {} - Device: {} - Headers: {}", correlationId, className, methodName,
				sanitizeArgs(joinPoint.getArgs()), deviceInfo, sanitizeHeaders(headers));

		long startTime = System.currentTimeMillis();
		try {
			Object result = joinPoint.proceed();
			long duration = System.currentTimeMillis() - startTime;

			log.info("[{}] Controller: {}.{} completed in {}ms", correlationId, className, methodName, duration);

			return result;
		} catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			log.error("[{}] Controller: {}.{} failed in {}ms - Error: {}", correlationId, className, methodName,
					duration, e.getMessage());
			throw e;
		}
	}

	@Around("execution(* com.whatsapp.service..*(..))")
	public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
		String correlationId = RequestContext.getCorrelationId();
		String methodName = joinPoint.getSignature().getName();
		String className = joinPoint.getTarget().getClass().getSimpleName();

		log.debug("[{}] Service: {}.{} - Args: {}", correlationId, className, methodName,
				sanitizeArgs(joinPoint.getArgs()));

		long startTime = System.currentTimeMillis();
		try {
			Object result = joinPoint.proceed();
			long duration = System.currentTimeMillis() - startTime;

			if (duration > 1000) {
				log.warn("[{}] Service: {}.{} completed in {}ms (slow)", correlationId, className, methodName,
						duration);
			} else {
				log.debug("[{}] Service: {}.{} completed in {}ms", correlationId, className, methodName, duration);
			}

			return result;
		} catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			log.error("[{}] Service: {}.{} failed in {}ms - Error: {}", correlationId, className, methodName, duration,
					e.getMessage());
			throw e;
		}
	}

	@AfterThrowing(pointcut = "execution(* com.whatsapp..*(..))", throwing = "ex")
	public void logException(JoinPoint joinPoint, Throwable ex) {
		String correlationId = RequestContext.getCorrelationId();
		String methodName = joinPoint.getSignature().getName();
		String className = joinPoint.getTarget().getClass().getSimpleName();

		log.error("[{}] Exception in {}.{}: {} - Args: {}", correlationId, className, methodName, ex.getMessage(),
				sanitizeArgs(joinPoint.getArgs()), ex);
	}

	private String sanitizeArgs(Object[] args) {
		if (args == null || args.length == 0) {
			return "[]";
		}

		return Arrays.stream(args).map(this::sanitizeArg).reduce((a, b) -> a + ", " + b).map(s -> "[" + s + "]")
				.orElse("[]");
	}

	private String sanitizeArg(Object arg) {
		if (arg == null)
			return "null";

		String className = arg.getClass().getSimpleName();

		if (arg instanceof String) {
			String str = (String) arg;
			return str.length() > 100 ? str.substring(0, 100) + "..." : str;
		}

		if (isPrimitiveOrWrapper(arg)) {
			return arg.toString();
		}

		return className + "@" + Integer.toHexString(arg.hashCode());
	}

	private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
		if (headers == null)
			return null;

		Map<String, String> sanitized = new java.util.HashMap<>(headers);

		if (sanitized.containsKey("authorization")) {
			String auth = sanitized.get("authorization");
			sanitized.put("authorization", auth.length() > 20 ? auth.substring(0, 20) + "..." : "***");
		}

		return sanitized;
	}

	private boolean isPrimitiveOrWrapper(Object obj) {
		return obj instanceof Number || obj instanceof Boolean || obj instanceof Character;
	}
}