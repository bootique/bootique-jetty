package io.bootique.jetty.instrumented;

import io.bootique.ConfigModule;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

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
