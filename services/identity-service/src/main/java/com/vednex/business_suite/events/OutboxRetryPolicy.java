package com.vednex.business_suite.events;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxRetryPolicy {

	private final int maxRetries;
	private final long retryBaseSeconds;

	public OutboxRetryPolicy(
			@Value("${app.outbox.max-retries}") int maxRetries,
			@Value("${app.outbox.retry-base-seconds}") long retryBaseSeconds
	) {
		this.maxRetries = maxRetries;
		this.retryBaseSeconds = retryBaseSeconds;
	}

	public boolean shouldRetry(int retryCountAfterFailure) {
		return retryCountAfterFailure < maxRetries;
	}

	public Instant nextRetryAt(Instant now, int retryCountAfterFailure) {
		int exponent = Math.max(0, retryCountAfterFailure - 1);
		long multiplier = 1L << Math.min(exponent, 10);
		return now.plusSeconds(retryBaseSeconds * multiplier);
	}

	public int maxRetries() {
		return maxRetries;
	}
}
