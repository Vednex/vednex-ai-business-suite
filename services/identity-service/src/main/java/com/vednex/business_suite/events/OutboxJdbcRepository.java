package com.vednex.business_suite.events;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxJdbcRepository {

	private static final String SENSITIVE_EVENT_REDACTION = """
			{"data":{"redacted":true,"redactedReason":"sensitive notification URL removed after broker confirm"}}
			""";

	private final JdbcTemplate jdbcTemplate;

	public OutboxJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void save(String aggregateType, UUID aggregateId, EventEnvelope envelope, String payloadJson) {
		jdbcTemplate.update("""
				insert into outbox_events (
					id, aggregate_type, aggregate_id, event_type, event_version, payload,
					company_id, user_id, correlation_id, status, created_at, next_retry_at
				)
				values (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, 'PENDING', ?, ?)
				""",
				envelope.eventId(),
				aggregateType,
				aggregateId,
				envelope.eventType(),
				envelope.eventVersion(),
				payloadJson,
				envelope.companyId(),
				envelope.userId(),
				envelope.correlationId(),
				Timestamp.from(envelope.occurredAt()),
				Timestamp.from(envelope.occurredAt()));
	}

	public List<OutboxEvent> lockPendingBatch(int batchSize) {
		return jdbcTemplate.query("""
				select id, event_type, event_version, payload::text, correlation_id, retry_count, created_at
				from outbox_events
				where status = 'PENDING'
				  and next_retry_at <= CURRENT_TIMESTAMP
				order by created_at
				limit ?
				for update skip locked
				""", this::mapEvent, batchSize);
	}

	public void markProcessing(UUID id) {
		jdbcTemplate.update("""
				update outbox_events
				set status = 'PROCESSING',
				    version = version + 1
				where id = ?
				""", id);
	}

	public void markPublished(UUID id, String eventType, Instant publishedAt) {
		if (isSensitiveNotificationEvent(eventType)) {
			jdbcTemplate.update("""
					update outbox_events
					set status = 'PUBLISHED',
					    published_at = ?,
					    last_error = null,
					    payload = payload || cast(? as jsonb),
					    version = version + 1
					where id = ?
					""", Timestamp.from(publishedAt), SENSITIVE_EVENT_REDACTION, id);
			return;
		}
		jdbcTemplate.update("""
				update outbox_events
				set status = 'PUBLISHED',
				    published_at = ?,
				    last_error = null,
				    version = version + 1
				where id = ?
				""", Timestamp.from(publishedAt), id);
	}

	public void markRetryable(UUID id, int retryCount, Instant nextRetryAt, String error) {
		jdbcTemplate.update("""
				update outbox_events
				set status = 'PENDING',
				    retry_count = ?,
				    next_retry_at = ?,
				    last_error = ?,
				    version = version + 1
				where id = ?
				""", retryCount, Timestamp.from(nextRetryAt), truncate(error), id);
	}

	public void markFailed(UUID id, int retryCount, String error) {
		jdbcTemplate.update("""
				update outbox_events
				set status = 'FAILED',
				    retry_count = ?,
				    last_error = ?,
				    version = version + 1
				where id = ?
				""", retryCount, truncate(error), id);
	}

	public long countPending() {
		Long count = jdbcTemplate.queryForObject("select count(*) from outbox_events where status = 'PENDING'", Long.class);
		return count == null ? 0L : count;
	}

	public long countFailed() {
		Long count = jdbcTemplate.queryForObject("select count(*) from outbox_events where status = 'FAILED'", Long.class);
		return count == null ? 0L : count;
	}

	private OutboxEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
		return new OutboxEvent(
				rs.getObject("id", UUID.class),
				rs.getString("event_type"),
				rs.getInt("event_version"),
				rs.getString("payload"),
				rs.getObject("correlation_id", UUID.class),
				rs.getInt("retry_count"),
				rs.getTimestamp("created_at").toInstant()
		);
	}

	private String truncate(String error) {
		if (error == null) {
			return null;
		}
		return error.length() <= 1000 ? error : error.substring(0, 1000);
	}

	private boolean isSensitiveNotificationEvent(String eventType) {
		return EventTypes.IDENTITY_EMAIL_VERIFICATION_REQUESTED_V1.equals(eventType)
				|| EventTypes.IDENTITY_PASSWORD_RESET_REQUESTED_V1.equals(eventType)
				|| EventTypes.IDENTITY_USER_INVITED_V1.equals(eventType);
	}
}
