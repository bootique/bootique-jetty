package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
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
	private Filter mockFilter1;
	private Filter mockFilter2;
	private Filter mockFilter3;
	private Module baseModule;
	private Module jettyModule;

	@Before
	public void before() {
		this.mockServlet = mock(Servlet.class);
		this.mockFilter1 = mock(Filter.class);
		this.mockFilter2 = mock(Filter.class);
		this.mockFilter3 = mock(Filter.class);

		this.baseModule = BQCoreModule.builder().args(new String[] { "a1", "a2" })
				.bootLogger(new DefaultBootLogger(false)).build();
		this.jettyModule = new JettyModule();
	}

	protected Server createServer(Module extModule) {
		return Guice.createInjector(baseModule, jettyModule, extModule).getInstance(Server.class);
	}

	@Test
	public void testContributeServlets() throws Exception {

		MappedServlet mappedServlet = new MappedServlet(mockServlet, new HashSet<>(Arrays.asList("/a/*", "/b/*")));

		Server server = createServer(binder -> {
			JettyModule.contributeServlets(binder).addBinding().toInstance(mappedServlet);
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
	public void testContributeFilters_InitDestroy() throws Exception {

		MappedFilter mf1 = new MappedFilter(mockFilter1, Collections.singleton("/a/*"), 10);
		MappedFilter mf2 = new MappedFilter(mockFilter2, Collections.singleton("/a/*"), 0);
		MappedFilter mf3 = new MappedFilter(mockFilter3, Collections.singleton("/a/*"), 5);

		Server server = createServer(binder -> {

			JettyModule.contributeFilters(binder).addBinding().toInstance(mf1);
			JettyModule.contributeFilters(binder).addBinding().toInstance(mf2);
			JettyModule.contributeFilters(binder).addBinding().toInstance(mf3);
		});

		try {
			server.start();

			Arrays.asList(mockFilter1, mockFilter2, mockFilter3).forEach(f -> {
				try {
					verify(f).init(any());
				} catch (Exception e) {
					fail("init failed");
				}
			});

		} finally {
			server.stop();
			Arrays.asList(mockFilter1, mockFilter2, mockFilter3).forEach(f -> verify(f).destroy());
		}
	}

	@Test
	public void testConfitributeFilters_Ordering() throws Exception {

		Filter[] mockFilters = new Filter[] { mockFilter1, mockFilter2, mockFilter3 };

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

		MappedFilter mf1 = new MappedFilter(mockFilter1, Collections.singleton("/a/*"), 10);
		MappedFilter mf2 = new MappedFilter(mockFilter2, Collections.singleton("/a/*"), 0);
		MappedFilter mf3 = new MappedFilter(mockFilter3, Collections.singleton("/a/*"), 5);

		MappedServlet mappedServlet = new MappedServlet(mockServlet, Collections.singleton("/a/*"));

		Server server = createServer(binder -> {

			JettyModule.contributeFilters(binder).addBinding().toInstance(mf1);
			JettyModule.contributeFilters(binder).addBinding().toInstance(mf2);
			JettyModule.contributeFilters(binder).addBinding().toInstance(mf3);

			// must have a servlet behind the filter chain...
			JettyModule.contributeServlets(binder).addBinding().toInstance(mappedServlet);
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

	@Test
	public void testContributeListeners_ServletContextListener() throws Exception {

		ServletContextListener scListener = mock(ServletContextListener.class);

		Server server = createServer(binder -> {
			JettyModule.contributeListeners(binder).addBinding().toInstance(scListener);
		});

		try {

			verify(scListener, times(0)).contextInitialized(any());
			verify(scListener, times(0)).contextDestroyed(any());

			server.start();

			verify(scListener).contextInitialized(any());
			verify(scListener, times(0)).contextDestroyed(any());

		} finally {
			server.stop();
			verify(scListener).contextInitialized(any());
			verify(scListener).contextDestroyed(any());
		}
	}

	@Test
	public void testContributeListeners_ServletRequestListener() throws Exception {

		ServletRequestListener srListener = mock(ServletRequestListener.class);

		Server server = createServer(binder -> {
			JettyModule.contributeListeners(binder).addBinding().toInstance(srListener);
		});

		try {

			verify(srListener, times(0)).requestInitialized(any());
			verify(srListener, times(0)).requestDestroyed(any());

			server.start();

			verify(srListener, times(0)).requestInitialized(any());
			verify(srListener, times(0)).requestDestroyed(any());

			WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

			base.path("/a").request().get();
			verify(srListener, times(1)).requestInitialized(any());
			verify(srListener, times(1)).requestDestroyed(any());

			base.path("/b").request().get();
			verify(srListener, times(2)).requestInitialized(any());
			verify(srListener, times(2)).requestDestroyed(any());

			// not_found request
			base.path("/c").request().get();
			verify(srListener, times(3)).requestInitialized(any());
			verify(srListener, times(3)).requestDestroyed(any());

		} finally {
			server.stop();
		}
	}

	// TODO: tests for Attribute listeners, session listeners

}
