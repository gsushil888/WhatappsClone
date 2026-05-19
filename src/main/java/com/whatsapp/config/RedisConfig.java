package com.whatsapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	@Bean
	RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
		if (!redisPassword.isEmpty()) {
			config.setPassword(redisPassword);
		}
		return new LettuceConnectionFactory(config);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void checkRedisConnection(ApplicationReadyEvent event) {
		RedisConnectionFactory factory = event.getApplicationContext().getBean(RedisConnectionFactory.class);
		try {
			factory.getConnection().ping();
			log.info("✅ Redis connected successfully at {}:{}", redisHost, redisPort);
		} catch (Exception e) {
			log.error("❌ Redis connection FAILED at {}:{} — {}", redisHost, redisPort, e.getMessage());
		}
	}

	@Bean
	RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		StringRedisSerializer str = new StringRedisSerializer();
		template.setKeySerializer(str);
		template.setValueSerializer(str);
		template.setHashKeySerializer(str);
		template.setHashValueSerializer(str);
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		StringRedisSerializer str = new StringRedisSerializer();
		template.setKeySerializer(str);
		template.setHashKeySerializer(str);
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}
}