package io.bootique.jetty;

import io.bootique.jetty.unit.JettyApp;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.Servlet;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MappedServletIT {

	private Servlet mockServlet;

	@Rule
	public JettyApp app = new JettyApp();

	@Before
	public void before() {
		this.mockServlet = mock(Servlet.class);
	}

	@Test
	public void testMappedConfig() throws Exception {

		MappedServlet mappedServlet = new MappedServlet(mockServlet, new HashSet<>(Arrays.asList("/a/*", "/b/*")));

		app.startServer(binder -> {
			JettyModule.contributeMappedServlets(binder).addBinding().toInstance(mappedServlet);
		});

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/a").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		Response r2 = base.path("/b").request().get();
		assertEquals(Status.OK.getStatusCode(), r2.getStatus());

		Response r3 = base.path("/c").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

		verify(mockServlet, times(2)).service(any(), any());
	}

	@Test
	public void testMappedConfig_Override() throws Exception {

		MappedServlet mappedServlet = new MappedServlet(mockServlet, new HashSet<>(Arrays.asList("/a/*", "/b/*")),
				"s1");

		app.startServer(binder -> {
			JettyModule.contributeMappedServlets(binder).addBinding().toInstance(mappedServlet);
		}, "--config=classpath:io/bootique/jetty/MappedServletIT1.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/a").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r1.getStatus());

		Response r2 = base.path("/b").request().get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), r2.getStatus());

		Response r3 = base.path("/c").request().get();
		assertEquals(Status.OK.getStatusCode(), r3.getStatus());

		verify(mockServlet).service(any(), any());
	}

}
