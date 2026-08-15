package com.vednex.business_suite.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxJdbcRepositoryTests {

	private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
	private final OutboxJdbcRepository repository = new OutboxJdbcRepository(jdbcTemplate);

	@Test
	void redactsSensitiveNotificationPayloadAfterPublish() {
		UUID eventId = UUID.randomUUID();

		repository.markPublished(eventId, EventTypes.IDENTITY_EMAIL_VERIFICATION_REQUESTED_V1, Instant.parse("2026-08-15T05:30:00Z"));

		verify(jdbcTemplate).update(
				org.mockito.ArgumentMatchers.contains("payload = payload || cast(? as jsonb)"),
				any(Timestamp.class),
				org.mockito.ArgumentMatchers.contains("sensitive notification URL removed"),
				eq(eventId)
		);
	}

	@Test
	void keepsNonSensitivePayloadAfterPublish() {
		UUID eventId = UUID.randomUUID();

		repository.markPublished(eventId, EventTypes.IDENTITY_USER_REGISTERED_V1, Instant.parse("2026-08-15T05:30:00Z"));

		verify(jdbcTemplate).update(
				org.mockito.ArgumentMatchers.argThat(sql -> sql != null && !sql.contains("payload = payload || cast(? as jsonb)")),
				any(Timestamp.class),
				eq(eventId)
		);
	}
}
