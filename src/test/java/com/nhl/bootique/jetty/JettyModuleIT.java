package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
	public void testFilters_InitDestroy() throws Exception {

		Filter[] mockFilters = new Filter[] { mock(Filter.class), mock(Filter.class), mock(Filter.class) };

		Server server = createServer(binder -> {
			JettyBinder.contributeTo(binder).filter(mockFilters[0], 10, "/a/*");
			JettyBinder.contributeTo(binder).filter(mockFilters[1], 0, "/a/*");
			JettyBinder.contributeTo(binder).filter(mockFilters[2], 5, "/a/*");
		});

		try {
			server.start();

			Arrays.asList(mockFilters).forEach(f -> {
				try {
					verify(f).init(any());
				} catch (Exception e) {
					fail("init failed");
				}
			});

		} finally {
			server.stop();
			Arrays.asList(mockFilters).forEach(f -> verify(f).destroy());
		}
	}

	@Test
	public void testFilters_Ordering() throws Exception {

		Filter[] mockFilters = new Filter[] { mock(Filter.class), mock(Filter.class), mock(Filter.class) };

		for (int i = 0; i < mockFilters.length; i++) {

			String responseString = Integer.toString(i);
			doAnswer(inv -> {

				HttpServletRequest request = inv.getArgumentAt(0, HttpServletRequest.class);
				HttpServletResponse response = inv.getArgumentAt(1, HttpServletResponse.class);
				FilterChain chain = inv.getArgumentAt(2, FilterChain.class);

				response.setStatus(200);
				response.getWriter().append(responseString);
				chain.doFilter(request, response);

				return null;
			}).when(mockFilters[i]).doFilter(any(), any(), any());
		}

		Server server = createServer(binder -> {
			JettyBinder.contributeTo(binder).filter(mockFilters[0], 10, "/a/*");
			JettyBinder.contributeTo(binder).filter(mockFilters[1], 0, "/a/*");
			JettyBinder.contributeTo(binder).filter(mockFilters[2], 5, "/a/*");

			// must have a servlet behind the filter chain...
			JettyBinder.contributeTo(binder).servlet(mockServlet, "/a/*");
		});

		try {
			server.start();

			WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

			Response response = base.path("/a").request().get();
			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			assertEquals("120", response.readEntity(String.class));

		} finally {
			server.stop();
		}
	}

}
