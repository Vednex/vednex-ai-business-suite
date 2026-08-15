package com.vednex.business_suite.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class RabbitTopologyConfig {

	private static final Logger log = LoggerFactory.getLogger(RabbitTopologyConfig.class);

	@Bean
	TopicExchange domainEventsExchange(@Value("${app.events.exchange}") String exchangeName) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	TopicExchange domainEventsDeadLetterExchange(@Value("${app.events.dead-letter-exchange}") String exchangeName) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	Queue identityEventsArchiveQueue(
			@Value("${app.events.archive-queue}") String queueName,
			@Value("${app.events.dead-letter-exchange}") String deadLetterExchange
	) {
		return QueueBuilder.durable(queueName)
				.deadLetterExchange(deadLetterExchange)
				.build();
	}

	@Bean
	Binding identityEventsArchiveBinding(Queue identityEventsArchiveQueue, TopicExchange domainEventsExchange) {
		return BindingBuilder.bind(identityEventsArchiveQueue)
				.to(domainEventsExchange)
				.with("identity.#");
	}

	@Bean
	Queue domainEventsDeadLetterQueue(@Value("${app.events.dead-letter-queue}") String queueName) {
		return QueueBuilder.durable(queueName).build();
	}

	@Bean
	Binding domainEventsDeadLetterBinding(Queue domainEventsDeadLetterQueue, TopicExchange domainEventsDeadLetterExchange) {
		return BindingBuilder.bind(domainEventsDeadLetterQueue)
				.to(domainEventsDeadLetterExchange)
				.with("#");
	}

	@Bean
	RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
		rabbitAdmin.setAutoStartup(true);
		return rabbitAdmin;
	}

	@Bean
	ApplicationRunner rabbitTopologyInitializer(RabbitAdmin rabbitAdmin) {
		return args -> {
			try {
				rabbitAdmin.initialize();
			} catch (RuntimeException ex) {
				log.warn("RabbitMQ topology declaration deferred because RabbitMQ is unavailable: {}", ex.getMessage());
			}
		};
	}
}
