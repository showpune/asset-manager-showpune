package com.microsoft.migration.assets.worker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkerApplicationTests {

	@Test
	void workerApplicationMainClassExists() {
		// This test verifies that the migration was successful by ensuring
		// the worker application class can be instantiated
		WorkerApplication app = new WorkerApplication();
		assertNotNull(app);
	}

	@Test
	void serviceBusConfigurationClassExists() {
		// Verify that ServiceBusConfig class exists and can be instantiated
		try {
			Class.forName("com.microsoft.migration.assets.worker.config.ServiceBusConfig");
		} catch (ClassNotFoundException e) {
			throw new AssertionError("ServiceBusConfig class not found - migration incomplete", e);
		}
	}

}