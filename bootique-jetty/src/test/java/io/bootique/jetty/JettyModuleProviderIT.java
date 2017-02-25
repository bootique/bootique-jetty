package io.bootique.jetty;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class JettyModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(JettyModuleProvider.class);
	}

	@Test
	public void testMetadata() {
	    BQModuleProviderChecker.testMetadata(JettyModuleProvider.class);
    }
}
