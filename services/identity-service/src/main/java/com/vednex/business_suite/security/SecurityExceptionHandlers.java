package com.vednex.business_suite.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vednex.business_suite.common.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityExceptionHandlers {

	@Bean
	AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
		return (request, response, exception) -> writeError(
				objectMapper,
				request,
				response,
				HttpStatus.UNAUTHORIZED,
				"UNAUTHENTICATED",
				"Authentication is required"
		);
	}

	@Bean
	AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
		return (request, response, exception) -> writeError(
				objectMapper,
				request,
				response,
				HttpStatus.FORBIDDEN,
				"FORBIDDEN",
				"You do not have permission to perform this action"
		);
	}

	private static void writeError(
			ObjectMapper objectMapper,
			HttpServletRequest request,
			HttpServletResponse response,
			HttpStatus status,
			String code,
			String message
	) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(code, message, request.getRequestURI()));
	}

}
