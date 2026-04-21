package com.whatsapp.config;

import com.whatsapp.filter.JwtAuthenticationFilter;
import com.whatsapp.filter.RequestContextFilter;
import com.whatsapp.util.JwtUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.whatsapp.repository.UserSessionRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public RequestContextFilter customRequestContextFilter() {
		return new RequestContextFilter();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil,
			UserSessionRepository userSessionRepository) {
		return new JwtAuthenticationFilter(jwtUtil, userSessionRepository);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtUtil jwtUtil,
			UserSessionRepository userSessionRepository)
			throws Exception {
		RequestContextFilter requestContextFilter = customRequestContextFilter();
		JwtAuthenticationFilter jwtAuthenticationFilter = jwtAuthenticationFilter(
				jwtUtil, userSessionRepository);

		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors
						.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth

						// ✅ Angular static content
						.requestMatchers("/", "/index.html", "/favicon.ico",
								"/*.js", "/*.css", "/assets/**")
						.permitAll()

						// ✅ Public APIs (login/register only)
						.requestMatchers("/api/v1/auth/**").permitAll()

						// 🔒 EVERYTHING ELSE = PROTECTED API
						.requestMatchers("/api/**").authenticated()

						// (optional health, ws, actuator)
						.requestMatchers("/ws/**", "/health", "/actuator/**")
						.permitAll()

						.anyRequest().authenticated())
				.addFilterBefore(requestContextFilter,
						UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(jwtAuthenticationFilter,
						RequestContextFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(
				List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
			AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
