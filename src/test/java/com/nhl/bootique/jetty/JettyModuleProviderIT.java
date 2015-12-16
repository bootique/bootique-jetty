package com.nhl.bootique.jetty;

import static org.junit.Assert.assertTrue;

import java.util.ServiceLoader;

import org.junit.Test;

import com.nhl.bootique.BQModuleProvider;

public class JettyModuleProviderIT {

	@Test
	public void testPresentInJar() {

		boolean[] found = { false };

		ServiceLoader.load(BQModuleProvider.class).forEach(p -> {
			if (p instanceof JettyModuleProvider) {
				found[0] = true;
			}
		});

		assertTrue(found[0]);
	}
}
