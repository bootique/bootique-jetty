package io.bootique.jetty;

import io.bootique.jetty.unit.JettyApp;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.Filter;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MappedFilterIT {

	private Filter mockFilter;

	@Rule
	public JettyApp app = new JettyApp();

	@Before
	public void before() {
		this.mockFilter = mock(Filter.class);
	}

	@Test
	public void testMappedConfig() throws Exception {

		app.start(binder -> JettyModule.extend(binder).addFilter(mockFilter, "f1", 0, "/a/*", "/b/*"));

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/a").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		Response r2 = base.path("/b").request().get();
		assertEquals(Status.OK.getStatusCode(), r2.getStatus());

		Response r3 = base.path("/c").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

		verify(mockFilter, times(2)).doFilter(any(), any(), any());
	}

	@Test
	public void testMappedConfig_Override() throws Exception {

		app.start(binder -> JettyModule.extend(binder).addFilter(mockFilter, "f1", 0, "/a/*", "/b/*"),
				"--config=classpath:io/bootique/jetty/MappedFilterIT1.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/a").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());

		Response r2 = base.path("/b").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r2.getStatus());

		Response r3 = base.path("/c").request().get();
		assertEquals(Status.OK.getStatusCode(), r3.getStatus());

		verify(mockFilter, times(1)).doFilter(any(), any(), any());
	}

}
