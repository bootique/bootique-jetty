package io.bootique.jetty;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class JettyModuleProviderTest {

	@Test
	public void testAutoLoadable() {
		BQModuleProviderChecker.testAutoLoadable(JettyModuleProvider.class);
	}

	@Test
	public void testMetadata() {
	    BQModuleProviderChecker.testMetadata(JettyModuleProvider.class);
    }
}
