package com.nhl.bootique.jetty.instrumented;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Test;

import com.nhl.bootique.ConfigModule;

public class InstrumentedJettyModuleTest {

	@Test
	public void testDefaultConstructor() {
		InstrumentedJettyModule m = new InstrumentedJettyModule();
		assertEquals("jetty", getConfigPrefix(m));
	}

	@Test
	public void testPrefixConstructor() {
		InstrumentedJettyModule m = new InstrumentedJettyModule("xyz");
		assertEquals("xyz", getConfigPrefix(m));
	}

	private static String getConfigPrefix(InstrumentedJettyModule module) {

		try {
			Field f = ConfigModule.class.getDeclaredField("configPrefix");
			f.setAccessible(true);
			return (String) f.get(module);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
