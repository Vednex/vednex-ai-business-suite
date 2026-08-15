package com.vednex.notification.email;

public final class EmailAddressMasker {

	private EmailAddressMasker() {
	}

	public static String mask(String email) {
		if (email == null || email.isBlank()) {
			return "";
		}
		int at = email.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		String local = email.substring(0, at);
		String domain = email.substring(at + 1);
		return local.charAt(0) + "***@" + domain;
	}
}
