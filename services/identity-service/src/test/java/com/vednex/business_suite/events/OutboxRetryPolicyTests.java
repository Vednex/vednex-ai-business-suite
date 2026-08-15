package com.vednex.business_suite.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OutboxRetryPolicyTests {

	@Test
	void calculatesBoundedExponentialBackoff() {
		OutboxRetryPolicy retryPolicy = new OutboxRetryPolicy(4, 30);
		Instant now = Instant.parse("2026-08-14T12:00:00Z");

		assertThat(retryPolicy.nextRetryAt(now, 1)).isEqualTo(now.plusSeconds(30));
		assertThat(retryPolicy.nextRetryAt(now, 2)).isEqualTo(now.plusSeconds(60));
		assertThat(retryPolicy.nextRetryAt(now, 3)).isEqualTo(now.plusSeconds(120));
		assertThat(retryPolicy.shouldRetry(3)).isTrue();
		assertThat(retryPolicy.shouldRetry(4)).isFalse();
	}
}
