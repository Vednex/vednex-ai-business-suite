package com.vednex.business_suite.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CorrelationIdsTests {

	@Test
	void preservesCorrelationIdFromGatewayHeader() {
		UUID correlationId = UUID.randomUUID();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Correlation-ID", correlationId.toString());

		assertThat(CorrelationIds.from(request)).isEqualTo(correlationId);
	}

	@Test
	void preservesCompatibilityHeader() {
		UUID correlationId = UUID.randomUUID();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Correlation-Id", correlationId.toString());

		assertThat(CorrelationIds.from(request)).isEqualTo(correlationId);
	}

	@Test
	void generatesCorrelationIdWhenMissingOrInvalid() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Correlation-ID", "not-a-uuid");

		assertThat(CorrelationIds.from(request)).isNotNull();
	}
}
