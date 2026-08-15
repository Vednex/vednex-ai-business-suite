package com.vednex.notification.email;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {

	public EmailMessage verificationEmail(String recipientEmail, String verificationUrl) {
		return new EmailMessage(
				recipientEmail,
				"Verify your Vednex account",
				"""
				Welcome to Vednex AI Business Suite.

				Verify your email by opening this link:
				%s

				If you did not create this account, ignore this email.
				""".formatted(verificationUrl),
				"EMAIL_VERIFICATION",
				"email-verification-v1"
		);
	}

	public EmailMessage passwordResetEmail(String recipientEmail, String resetUrl) {
		return new EmailMessage(
				recipientEmail,
				"Reset your Vednex password",
				"""
				Reset your Vednex password using this link:
				%s

				If you did not request a password reset, ignore this email.
				""".formatted(resetUrl),
				"PASSWORD_RESET",
				"password-reset-v1"
		);
	}

	public EmailMessage invitationEmail(String recipientEmail, String invitationUrl) {
		return new EmailMessage(
				recipientEmail,
				"You are invited to Vednex",
				"""
				You have been invited to join a company in Vednex AI Business Suite.

				Accept the invitation here:
				%s
				""".formatted(invitationUrl),
				"USER_INVITATION",
				"user-invitation-v1"
		);
	}
}
