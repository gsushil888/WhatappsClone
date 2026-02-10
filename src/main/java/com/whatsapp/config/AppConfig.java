package com.whatsapp.config;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {

	@Bean(name = "taskExecutor")
	public ThreadPoolTaskExecutor taskExecutor(TaskDecorator mdcTaskDecorator) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("WhatsApp-Async-");
		executor.setTaskDecorator(mdcTaskDecorator);
		executor.initialize();
		return executor;
	}

	@Bean
	public TaskDecorator mdcTaskDecorator() {
		return new MdcTaskDecorator();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5);
		scheduler.setThreadNamePrefix("WhatsApp-Scheduler-");
		scheduler.initialize();
		return scheduler;
	}
}

class MdcTaskDecorator implements TaskDecorator {
	@Override
	public @NonNull Runnable decorate(@NonNull Runnable runnable) {
		var contextMap = MDC.getCopyOfContextMap();
		return () -> {
			Map<String, String> previous = MDC.getCopyOfContextMap();
			try {
				if (contextMap != null)
					MDC.setContextMap(contextMap);
				runnable.run();
			} finally {
				if (previous != null)
					MDC.setContextMap(previous);
				else
					MDC.clear();
			}
		};
	}
}