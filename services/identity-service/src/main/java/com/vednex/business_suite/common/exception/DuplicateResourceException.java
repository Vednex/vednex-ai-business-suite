package com.vednex.business_suite.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {
	public DuplicateResourceException(String code, String message) {
		super(code, message, HttpStatus.CONFLICT);
	}
}
