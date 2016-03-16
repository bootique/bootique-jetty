package com.nhl.bootique.jetty.instrumented;

import org.junit.Test;

import com.nhl.bootique.test.junit.BQModuleProviderChecker;

public class InstrumentedJettyModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(InstrumentedJettyModuleProvider.class);
	}
}
