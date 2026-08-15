package com.vednex.notification.delivery;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationDeliveryRepository {

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final String consumerName;

	public NotificationDeliveryRepository(
			JdbcTemplate jdbcTemplate,
			Clock clock,
			@Value("${app.notifications.consumer-name}") String consumerName) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.consumerName = consumerName;
	}

	public boolean isProcessed(UUID eventId) {
		Long count = jdbcTemplate.queryForObject("""
				select count(*) from processed_events
				where event_id = ? and consumer_name = ?
				""", Long.class, eventId, consumerName);
		return count != null && count > 0;
	}

	public Optional<NotificationStatus> currentStatus(UUID eventId) {
		return jdbcTemplate.query("""
				select status from notification_deliveries where event_id = ?
				""", rs -> rs.next() ? Optional.of(NotificationStatus.valueOf(rs.getString("status"))) : Optional.empty(), eventId);
	}

	@Transactional
	public boolean begin(NotificationDelivery delivery) {
		if (isProcessed(delivery.eventId())) {
			return false;
		}
		try {
			jdbcTemplate.update("""
					insert into notification_deliveries (
					    event_id, event_type, notification_type, channel, recipient, template_key, status, updated_at
					) values (?, ?, ?, ?, ?, ?, 'PROCESSING', ?)
					""",
					delivery.eventId(),
					delivery.eventType(),
					delivery.notificationType(),
					delivery.channel(),
					delivery.recipient(),
					delivery.templateKey(),
					Timestamp.from(Instant.now(clock)));
			return true;
		}
		catch (DuplicateKeyException ex) {
			return currentStatus(delivery.eventId()).map(status -> status != NotificationStatus.SENT).orElse(false);
		}
	}

	public int incrementAttempt(UUID eventId) {
		Integer attempts = jdbcTemplate.queryForObject("""
				update notification_deliveries
				set attempt_count = attempt_count + 1,
				    status = 'PROCESSING',
				    updated_at = ?
				where event_id = ?
				returning attempt_count
				""", Integer.class, Timestamp.from(Instant.now(clock)), eventId);
		return attempts == null ? 0 : attempts;
	}

	@Transactional
	public void markSentAndProcessed(UUID eventId, String providerMessageId) {
		Instant now = Instant.now(clock);
		jdbcTemplate.update("""
				update notification_deliveries
				set status = 'SENT',
				    provider_message_id = ?,
				    sent_at = ?,
				    updated_at = ?,
				    last_error = null
				where event_id = ?
				""", providerMessageId, Timestamp.from(now), Timestamp.from(now), eventId);
		jdbcTemplate.update("""
				insert into processed_events (event_id, consumer_name, processed_at)
				values (?, ?, ?)
				on conflict (event_id, consumer_name) do nothing
				""", eventId, consumerName, Timestamp.from(now));
	}

	public void markFailed(UUID eventId, NotificationStatus status, String error) {
		jdbcTemplate.update("""
				update notification_deliveries
				set status = ?,
				    last_error = ?,
				    updated_at = ?
				where event_id = ?
				""", status.name(), safeError(error), Timestamp.from(Instant.now(clock)), eventId);
	}

	private String safeError(String error) {
		if (error == null || error.isBlank()) {
			return null;
		}
		return error.length() > 1000 ? error.substring(0, 1000) : error;
	}
}
