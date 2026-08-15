package com.vednex.business_suite.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {
	public BusinessRuleException(String code, String message) {
		super(code, message, HttpStatus.CONFLICT);
	}
}
