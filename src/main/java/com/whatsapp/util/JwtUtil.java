package com.whatsapp.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

	private final SecretKey secretKey;
	private final long accessTokenExpiration;
	private final long refreshTokenExpiration;

	public JwtUtil(@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-validity:900000}") long accessTokenExpiration,
			@Value("${jwt.refresh-token-validity:86400000}") long refreshTokenExpiration) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
		this.accessTokenExpiration = accessTokenExpiration;
		this.refreshTokenExpiration = refreshTokenExpiration;
	}

	public String generateAccessToken(Long userId, String sessionId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("sessionId", sessionId);
		claims.put("tokenType", "ACCESS");

		return Jwts.builder()
				.setClaims(claims)
				.setSubject(userId.toString())
				.setIssuedAt(new Date())
				.setExpiration(Date.from(Instant.now().plus(accessTokenExpiration, ChronoUnit.MILLIS)))
				.signWith(secretKey, SignatureAlgorithm.HS256)
				.compact();
	}

	public String generateRefreshToken(Long userId, String sessionId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("sessionId", sessionId);
		claims.put("tokenType", "REFRESH");

		return Jwts.builder()
				.setClaims(claims)
				.setSubject(userId.toString())
				.setIssuedAt(new Date())
				.setExpiration(Date.from(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS)))
				.signWith(secretKey, SignatureAlgorithm.HS256)
				.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("JWT token expired: {}", e.getMessage());
			return false;
		} catch (JwtException | IllegalArgumentException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
			return false;
		}
	}

	public Claims getClaimsFromToken(String token) {
		return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
	}

	public Long getUserIdFromToken(String token) {
		Claims claims = getClaimsFromToken(token);
		return Long.valueOf(claims.getSubject());
	}

	public String getSessionIdFromToken(String token) {
		Claims claims = getClaimsFromToken(token);
		return claims.get("sessionId", String.class);
	}

	public String getDeviceIdFromToken(String token) {
		Claims claims = getClaimsFromToken(token);
		return claims.get("deviceId", String.class);
	}

	public boolean isTokenExpired(String token) {
		try {
			Claims claims = getClaimsFromToken(token);
			return claims.getExpiration().before(new Date());
		} catch (ExpiredJwtException e) {
			return true;
		} catch (Exception e) {
			return true;
		}
	}
}