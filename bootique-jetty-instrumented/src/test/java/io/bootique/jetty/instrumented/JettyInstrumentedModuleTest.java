package io.bootique.jetty.instrumented;

import io.bootique.ConfigModule;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class JettyInstrumentedModuleTest {

	@Test
	public void testDefaultConstructor() {
		JettyInstrumentedModule m = new JettyInstrumentedModule();
		assertEquals("jetty", getConfigPrefix(m));
	}

	@Test
	public void testPrefixConstructor() {
		JettyInstrumentedModule m = new JettyInstrumentedModule("xyz");
		assertEquals("xyz", getConfigPrefix(m));
	}

	private static String getConfigPrefix(JettyInstrumentedModule module) {

		try {
			Field f = ConfigModule.class.getDeclaredField("configPrefix");
			f.setAccessible(true);
			return (String) f.get(module);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
