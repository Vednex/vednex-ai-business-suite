package com.vednex.gateway.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeansConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
