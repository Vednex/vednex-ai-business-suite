package com.vednex.notification.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vednex.notification.email.EmailAddressMasker;
import com.vednex.notification.email.EmailMessage;
import com.vednex.notification.email.PermanentEmailDeliveryException;
import com.vednex.notification.events.EventEnvelope;
import com.vednex.notification.events.EventEnvelopeValidator;
import com.vednex.notification.events.EventValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

	private final ObjectMapper objectMapper;
	private final EventEnvelopeValidator validator;
	private final NotificationEventMapper mapper;
	private final NotificationDeliveryRepository repository;
	private final EmailDeliveryService deliveryService;
	private final NotificationMetrics metrics;

	public NotificationEventConsumer(
			ObjectMapper objectMapper,
			EventEnvelopeValidator validator,
			NotificationEventMapper mapper,
			NotificationDeliveryRepository repository,
			EmailDeliveryService deliveryService,
			NotificationMetrics metrics) {
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.mapper = mapper;
		this.repository = repository;
		this.deliveryService = deliveryService;
		this.metrics = metrics;
	}

	@RabbitListener(queues = "${app.notifications.identity-queue}")
	public void consume(String body) {
		EventEnvelope event = parse(body);
		validate(event);
		EmailMessage message = map(event);
		NotificationDelivery delivery = NotificationDelivery.from(event, message);
		metrics.received(delivery.notificationType());
		if (repository.isProcessed(event.eventId())) {
			metrics.duplicate(delivery.notificationType());
			log.info("Duplicate notification event acknowledged service=notification-service eventId={} eventType={} correlationId={} notificationType={} status=DUPLICATE",
					event.eventId(), event.eventType(), event.correlationId(), delivery.notificationType());
			return;
		}
		if (!repository.begin(delivery)) {
			metrics.duplicate(delivery.notificationType());
			log.info("Notification event already in terminal state service=notification-service eventId={} eventType={} correlationId={} notificationType={} status=DUPLICATE",
					event.eventId(), event.eventType(), event.correlationId(), delivery.notificationType());
			return;
		}
		log.info("Notification delivery started service=notification-service eventId={} eventType={} correlationId={} notificationType={} recipient={} status=PROCESSING",
				event.eventId(), event.eventType(), event.correlationId(), delivery.notificationType(), EmailAddressMasker.mask(delivery.recipient()));
		deliveryService.send(delivery, message);
		metrics.processed(delivery.notificationType());
		log.info("Notification delivery completed service=notification-service eventId={} eventType={} correlationId={} notificationType={} status=SENT",
				event.eventId(), event.eventType(), event.correlationId(), delivery.notificationType());
	}

	private EventEnvelope parse(String body) {
		try {
			return objectMapper.readValue(body, EventEnvelope.class);
		}
		catch (JsonProcessingException ex) {
			throw new AmqpRejectAndDontRequeueException("Malformed notification event JSON", ex);
		}
	}

	private EmailMessage map(EventEnvelope event) {
		try {
			return mapper.toEmailMessage(event);
		}
		catch (EventValidationException | PermanentEmailDeliveryException ex) {
			throw new AmqpRejectAndDontRequeueException("Unsupported or invalid notification event", ex);
		}
	}

	private void validate(EventEnvelope event) {
		try {
			validator.validate(event);
		}
		catch (EventValidationException ex) {
			throw new AmqpRejectAndDontRequeueException("Invalid notification event envelope", ex);
		}
	}
}
