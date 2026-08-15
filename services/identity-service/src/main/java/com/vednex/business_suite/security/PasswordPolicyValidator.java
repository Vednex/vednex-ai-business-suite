package com.vednex.business_suite.security;

import com.vednex.business_suite.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

	public void validate(String password) {
		if (password == null || password.length() < 8 || password.length() > 100) {
			throw new BadRequestException("WEAK_PASSWORD", "Password must be between 8 and 100 characters");
		}
		if (!password.matches(".*[A-Z].*")
				|| !password.matches(".*[a-z].*")
				|| !password.matches(".*\\d.*")) {
			throw new BadRequestException("WEAK_PASSWORD", "Password must include uppercase, lowercase, and numeric characters");
		}
	}

}
