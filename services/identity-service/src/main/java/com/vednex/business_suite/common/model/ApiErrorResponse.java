package com.vednex.business_suite.common.model;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		boolean success,
		String code,
		String message,
		List<FieldErrorResponse> fieldErrors,
		Instant timestamp,
		String path
) {

	public static ApiErrorResponse of(String code, String message, String path) {
		return new ApiErrorResponse(false, code, message, List.of(), Instant.now(), path);
	}

	public static ApiErrorResponse of(String code, String message, List<FieldErrorResponse> fieldErrors, String path) {
		return new ApiErrorResponse(false, code, message, fieldErrors, Instant.now(), path);
	}

}
