package com.nhl.bootique.jetty.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;

import org.junit.Test;

public class ServerFactoryStaticsTest {

	@Test
	public void testPreResolveResourceUrl_Null() {
		assertNull(ServerFactory.preResolveResourceUrl(null));
	}

	@Test
	public void testPreResolveResourceUrl_Path() {
		assertEquals("com/nhl/bootique/jetty/server",
				ServerFactory.preResolveResourceUrl("com/nhl/bootique/jetty/server"));
	}

	@Test
	public void testPreResolveResourceUrl_Classpath() {

		URL url = ServerFactoryStaticsTest.class.getClassLoader().getResource("com/nhl/bootique/jetty/server");
		assertNotNull(url);
		assertEquals(url.toString(), ServerFactory.preResolveResourceUrl("classpath:com/nhl/bootique/jetty/server"));
	}

	@Test
	public void testPreResolveResourceUrl_ClasspathMissing() {
		assertEquals("classpath:com/nhl/bootique/jetty/no_such_url",
				ServerFactory.preResolveResourceUrl("classpath:com/nhl/bootique/jetty/no_such_url"));
	}
}
