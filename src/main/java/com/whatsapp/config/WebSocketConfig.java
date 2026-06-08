package com.whatsapp.config;

import com.whatsapp.interceptor.WebSocketAuthInterceptor;
import com.whatsapp.interceptor.WebSocketChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final WebSocketAuthInterceptor webSocketAuthInterceptor;
	private final WebSocketChannelInterceptor webSocketChannelInterceptor;

	@Override
	public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/queue");
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
		registration.interceptors(webSocketChannelInterceptor);
	}

	@Override
	public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
		// Main WebSocket endpoint with SockJS
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").addInterceptors(webSocketAuthInterceptor)
				.withSockJS();

		// Main WebSocket endpoint without SockJS
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").addInterceptors(webSocketAuthInterceptor);

		// STOMP WebSocket endpoint with SockJS
		registry.addEndpoint("/ws/stomp").setAllowedOriginPatterns("*").addInterceptors(webSocketAuthInterceptor)
				.withSockJS();

		// STOMP WebSocket endpoint without SockJS
		registry.addEndpoint("/ws/stomp").setAllowedOriginPatterns("*").addInterceptors(webSocketAuthInterceptor);
	}
}
