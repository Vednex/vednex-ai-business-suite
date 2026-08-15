package com.vednex.business_suite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

class TestcontainersDockerSmokeTests {

	@Test
	void dockerShouldBeAvailableToTestcontainers() {
		assertThat(DockerClientFactory.instance().isDockerAvailable()).isTrue();
	}

	@Test
	void postgresContainerShouldStart() {
		try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")) {
			postgres.start();

			assertThat(postgres.isRunning()).isTrue();
			assertThat(postgres.getMappedPort(5432)).isPositive();
			assertThat(postgres.getJdbcUrl()).startsWith("jdbc:postgresql://");
		}
	}
}
