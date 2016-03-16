package com.nhl.bootique.jetty;

import org.junit.Test;

import com.nhl.bootique.test.junit.BQModuleProviderChecker;

public class JettyModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(JettyModuleProvider.class);
	}
}
