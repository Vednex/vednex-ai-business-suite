package com.vednex.notification.delivery;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

	private final MeterRegistry meterRegistry;

	public NotificationMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void received(String notificationType) {
		counter("notification.received", notificationType).increment();
	}

	public void processed(String notificationType) {
		counter("notification.processed", notificationType).increment();
	}

	public void sent(String notificationType) {
		counter("notification.sent", notificationType).increment();
	}

	public void failed(String notificationType) {
		counter("notification.failed", notificationType).increment();
	}

	public void duplicate(String notificationType) {
		counter("notification.duplicate", notificationType).increment();
	}

	public void deadlettered(String notificationType) {
		counter("notification.deadlettered", notificationType).increment();
	}

	private Counter counter(String name, String notificationType) {
		return Counter.builder(name)
				.tag("notificationType", notificationType)
				.tag("channel", "EMAIL")
				.register(meterRegistry);
	}
}
