package com.whatsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = { AopAutoConfiguration.class })
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class WhatappsBackendLatestApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatappsBackendLatestApplication.class, args);
	}

}