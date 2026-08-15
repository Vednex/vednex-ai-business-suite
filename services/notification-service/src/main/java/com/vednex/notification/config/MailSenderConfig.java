package com.vednex.notification.config;

import com.vednex.notification.email.DevelopmentEmailSender;
import com.vednex.notification.email.EmailSender;
import com.vednex.notification.email.SmtpEmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailSenderConfig {

	@Bean
	EmailSender emailSender(
			ObjectProvider<JavaMailSender> mailSender,
			@Value("${spring.mail.host:}") String host,
			@Value("${app.mail.from}") String from,
			@Value("${app.mail.log-token-links:false}") boolean logTokenLinks) {
		if (host == null || host.isBlank()) {
			return new DevelopmentEmailSender(logTokenLinks);
		}
		return new SmtpEmailSender(mailSender.getObject(), from);
	}
}
