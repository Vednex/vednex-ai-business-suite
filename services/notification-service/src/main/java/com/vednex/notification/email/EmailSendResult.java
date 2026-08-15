package com.vednex.notification.email;

public record EmailSendResult(String providerMessageId) {

	public static EmailSendResult accepted() {
		return new EmailSendResult(null);
	}
}
