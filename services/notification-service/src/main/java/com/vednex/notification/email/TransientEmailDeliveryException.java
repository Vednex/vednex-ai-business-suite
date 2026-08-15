package com.vednex.notification.email;

public class TransientEmailDeliveryException extends RuntimeException {

	public TransientEmailDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}
}
