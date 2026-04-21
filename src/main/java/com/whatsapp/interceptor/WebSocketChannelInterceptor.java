package com.whatsapp.interceptor;

import java.util.Collections;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && accessor.getSessionAttributes() != null) {
      Object userId = accessor.getSessionAttributes().get("userId");

      if (userId != null) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId.toString(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        // Set in both accessor and SecurityContext
        accessor.setUser(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("WebSocket user authenticated: {} for command: {}", userId,
            accessor.getCommand());
      } else {
        log.warn("No userId found in session attributes for command: {}", accessor.getCommand());
      }
    }

    return message;
  }
}
