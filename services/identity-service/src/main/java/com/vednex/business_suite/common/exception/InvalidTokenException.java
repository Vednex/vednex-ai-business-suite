package com.vednex.business_suite.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {
	public InvalidTokenException(String message) {
		super("INVALID_TOKEN", message, HttpStatus.UNAUTHORIZED);
	}

	public InvalidTokenException(String code, String message) {
		super(code, message, HttpStatus.UNAUTHORIZED);
	}
}
