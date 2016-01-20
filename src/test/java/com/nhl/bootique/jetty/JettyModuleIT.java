package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.servlet.Servlet;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Module;
import com.nhl.bootique.BQCoreModule;
import com.nhl.bootique.log.DefaultBootLogger;

public class JettyModuleIT {

	private Servlet mockServlet;
	private Module baseModule;
	private Module jettyModule;

	@Before
	public void before() {
		this.mockServlet = mock(Servlet.class);
		this.baseModule = new BQCoreModule(new String[] { "a1", "a2" }, new DefaultBootLogger(false));
		this.jettyModule = new JettyModule();
	}

	protected Server createServer(Module extModule) {
		return Guice.createInjector(baseModule, jettyModule, extModule).getInstance(Server.class);
	}

	@Test
	public void testServlets() throws Exception {
		Server server = createServer(binder -> {
			JettyBinder.contributeTo(binder).servlet(mockServlet, "/a/*", "/b/*");
		});

		try {
			server.start();
			verify(mockServlet).init(any());

			WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

			Response r1 = base.path("/a").request().get();
			assertEquals(Status.OK.getStatusCode(), r1.getStatus());

			Response r2 = base.path("/b").request().get();
			assertEquals(Status.OK.getStatusCode(), r2.getStatus());

			Response r3 = base.path("/c").request().get();
			assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

		} finally {
			server.stop();
			verify(mockServlet).destroy();
		}
	}
	
	@Test
	public void testFilters() throws Exception {
		Server server = createServer(binder -> {
			JettyBinder.contributeTo(binder).servlet(mockServlet, "/a/*", "/b/*");
		});

		try {
			server.start();
			verify(mockServlet).init(any());

			WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

			Response r1 = base.path("/a").request().get();
			assertEquals(Status.OK.getStatusCode(), r1.getStatus());

			Response r2 = base.path("/b").request().get();
			assertEquals(Status.OK.getStatusCode(), r2.getStatus());

			Response r3 = base.path("/c").request().get();
			assertEquals(Status.NOT_FOUND.getStatusCode(), r3.getStatus());

		} finally {
			server.stop();
			verify(mockServlet).destroy();
		}
	}

}
