package com.vednex.business_suite.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

	private final Counter published;
	private final Counter retry;
	private final Counter failed;

	public OutboxMetrics(MeterRegistry meterRegistry, OutboxJdbcRepository outboxRepository) {
		this.published = Counter.builder("outbox.published").register(meterRegistry);
		this.retry = Counter.builder("outbox.retry").register(meterRegistry);
		this.failed = Counter.builder("outbox.failed").register(meterRegistry);
		Gauge.builder("outbox.pending", outboxRepository, OutboxJdbcRepository::countPending).register(meterRegistry);
		Gauge.builder("outbox.failed.current", outboxRepository, OutboxJdbcRepository::countFailed).register(meterRegistry);
	}

	public void published() {
		published.increment();
	}

	public void retry() {
		retry.increment();
	}

	public void failed() {
		failed.increment();
	}
}
