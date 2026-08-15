package com.vednex.business_suite.events;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

	private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

	private final OutboxJdbcRepository outboxRepository;
	private final OutboxRetryPolicy retryPolicy;
	private final OutboxMetrics metrics;
	private final RabbitTemplate rabbitTemplate;
	private final Clock clock;
	private final String exchange;
	private final int batchSize;
	private final long publishConfirmTimeoutMs;

	public OutboxPublisher(
			OutboxJdbcRepository outboxRepository,
			OutboxRetryPolicy retryPolicy,
			OutboxMetrics metrics,
			RabbitTemplate rabbitTemplate,
			Clock clock,
			@Value("${app.events.exchange}") String exchange,
			@Value("${app.outbox.batch-size}") int batchSize,
			@Value("${app.outbox.publish-confirm-timeout-ms}") long publishConfirmTimeoutMs
	) {
		this.outboxRepository = outboxRepository;
		this.retryPolicy = retryPolicy;
		this.metrics = metrics;
		this.rabbitTemplate = rabbitTemplate;
		this.clock = clock;
		this.exchange = exchange;
		this.batchSize = batchSize;
		this.publishConfirmTimeoutMs = publishConfirmTimeoutMs;
	}

	@Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
	@Transactional
	public void publishPendingEvents() {
		List<OutboxEvent> events = outboxRepository.lockPendingBatch(batchSize);
		for (OutboxEvent event : events) {
			publishOne(event);
		}
	}

	private void publishOne(OutboxEvent event) {
		outboxRepository.markProcessing(event.id());
		try {
			rabbitTemplate.invoke(operations -> {
				operations.convertAndSend(exchange, event.eventType(), event.payload(), message -> {
					MessageProperties properties = message.getMessageProperties();
					properties.setContentType(MediaType.APPLICATION_JSON_VALUE);
					properties.setHeader("eventId", event.id().toString());
					properties.setHeader("eventType", event.eventType());
					properties.setHeader("eventVersion", event.eventVersion());
					properties.setHeader("correlationId", event.correlationId().toString());
					properties.setHeader("producer", "identity-service");
					properties.setCorrelationId(event.correlationId().toString());
					return message;
				});
				operations.waitForConfirmsOrDie(publishConfirmTimeoutMs);
				return true;
			});
			outboxRepository.markPublished(event.id(), event.eventType(), Instant.now(clock));
			metrics.published();
		}
		catch (Exception ex) {
			handleFailure(event, ex);
		}
	}

	private void handleFailure(OutboxEvent event, Exception ex) {
		int retryCount = event.retryCount() + 1;
		String safeError = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "publish failed" : ex.getMessage());
		if (retryPolicy.shouldRetry(retryCount)) {
			outboxRepository.markRetryable(event.id(), retryCount, retryPolicy.nextRetryAt(Instant.now(clock), retryCount), safeError);
			metrics.retry();
			log.warn("Outbox publish failed; eventId={} eventType={} retryCount={} maxRetries={}",
					event.id(), event.eventType(), retryCount, retryPolicy.maxRetries());
		}
		else {
			outboxRepository.markFailed(event.id(), retryCount, safeError);
			metrics.failed();
			log.error("Outbox event failed permanently; eventId={} eventType={} retryCount={}",
					event.id(), event.eventType(), retryCount);
		}
	}
}
