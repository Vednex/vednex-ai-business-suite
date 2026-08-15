package com.vednex.business_suite;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusinessSuiteApplication {

	static {
		configureTimeZone();
	}

	public static void main(String[] args) {
		configureTimeZone();
		SpringApplication.run(BusinessSuiteApplication.class, args);
	}

	static void configureTimeZone() {
		System.setProperty("user.timezone", "UTC");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

}
