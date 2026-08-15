package com.vednex.business_suite.common.web;

import java.util.List;

import com.vednex.business_suite.common.exception.ApiException;
import com.vednex.business_suite.common.model.ApiErrorResponse;
import com.vednex.business_suite.common.model.FieldErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
		return ResponseEntity.status(ex.getStatus())
				.body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest()
				.body(ApiErrorResponse.of("VALIDATION_ERROR", "Request validation failed", fieldErrors, request.getRequestURI()));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiErrorResponse.of("UNAUTHENTICATED", "Authentication is required", request.getRequestURI()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiErrorResponse.of("FORBIDDEN", "You do not have permission to perform this action", request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected API error on {}", request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "An unexpected error occurred", request.getRequestURI()));
	}

}
