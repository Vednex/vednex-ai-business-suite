package com.vednex.business_suite.config;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeansConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper()
				.findAndRegisterModules()
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

}
