package com.vednex.business_suite.events;

import java.util.Map;
import java.util.UUID;

public interface DomainEventPublisher {

	void publish(
			String aggregateType,
			UUID aggregateId,
			String eventType,
			int eventVersion,
			UUID companyId,
			UUID userId,
			UUID correlationId,
			Map<String, Object> data
	);
}
