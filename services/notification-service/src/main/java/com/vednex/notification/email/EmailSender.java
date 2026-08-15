package com.vednex.notification.email;

public interface EmailSender {

	EmailSendResult send(EmailMessage message);
}
