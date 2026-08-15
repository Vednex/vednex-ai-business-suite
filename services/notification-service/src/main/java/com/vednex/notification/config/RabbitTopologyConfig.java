package com.vednex.notification.config;

import java.util.Map;

import com.vednex.notification.events.EventTypes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

	@Bean
	TopicExchange eventsExchange(@Value("${app.events.exchange}") String exchangeName) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	TopicExchange eventsDeadLetterExchange(@Value("${app.events.dead-letter-exchange}") String exchangeName) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	Queue notificationIdentityQueue(
			@Value("${app.notifications.identity-queue}") String queueName,
			@Value("${app.events.dead-letter-exchange}") String deadLetterExchange) {
		return new Queue(queueName, true, false, false, Map.of("x-dead-letter-exchange", deadLetterExchange));
	}

	@Bean
	Queue eventsDeadLetterQueue(@Value("${app.events.dead-letter-queue}") String queueName) {
		return new Queue(queueName, true);
	}

	@Bean
	Binding verificationRequestedBinding(Queue notificationIdentityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(notificationIdentityQueue).to(eventsExchange)
				.with(EventTypes.EMAIL_VERIFICATION_REQUESTED_V1);
	}

	@Bean
	Binding passwordResetRequestedBinding(Queue notificationIdentityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(notificationIdentityQueue).to(eventsExchange)
				.with(EventTypes.PASSWORD_RESET_REQUESTED_V1);
	}

	@Bean
	Binding userInvitedBinding(Queue notificationIdentityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(notificationIdentityQueue).to(eventsExchange)
				.with(EventTypes.USER_INVITED_V1);
	}

	@Bean
	Binding deadLetterBinding(Queue eventsDeadLetterQueue, TopicExchange eventsDeadLetterExchange) {
		return BindingBuilder.bind(eventsDeadLetterQueue).to(eventsDeadLetterExchange).with("#");
	}
}
