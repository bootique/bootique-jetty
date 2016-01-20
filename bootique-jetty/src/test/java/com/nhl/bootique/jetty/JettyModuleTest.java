package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Test;

import com.nhl.bootique.ConfigModule;

public class JettyModuleTest {

	@Test
	public void testDefaultConstructor() {
		JettyModule m = new JettyModule();
		assertEquals("jetty", getConfigPrefix(m));
	}

	@Test
	public void testPrefixConstructor() {
		JettyModule m = new JettyModule("xyz");
		assertEquals("xyz", getConfigPrefix(m));
	}

	private static String getConfigPrefix(JettyModule module) {

		try {
			Field f = ConfigModule.class.getDeclaredField("configPrefix");
			f.setAccessible(true);
			return (String) f.get(module);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
