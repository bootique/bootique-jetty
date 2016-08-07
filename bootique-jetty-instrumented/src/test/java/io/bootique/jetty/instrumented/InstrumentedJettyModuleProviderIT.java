package io.bootique.jetty.instrumented;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class InstrumentedJettyModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(InstrumentedJettyModuleProvider.class);
	}
}
