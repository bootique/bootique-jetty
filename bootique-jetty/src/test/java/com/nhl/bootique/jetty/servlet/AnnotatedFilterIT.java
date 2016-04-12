package com.nhl.bootique.jetty.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.unit.JettyApp;

public class AnnotatedFilterIT {

	private Runnable assertion;

	@Rule
	public JettyApp app = new JettyApp();

	@After
	public void after() {
		assertion = null;
	}

	@Test
	public void testServletContatinerState() throws Exception {
		app.startServer(new FilterModule());

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		assertNull(assertion);

		base.path("/b").request().get().close();
		assertNotNull(assertion);
		assertion = null;

		base.path("/b/1").request().get().close();
		assertNotNull(assertion);
		assertion = null;

		base.path("/b/2").request().get().close();
		assertNotNull(assertion);
	}

	class FilterModule implements Module {

		@Override
		public void configure(Binder binder) {
			JettyModule.contributeFilters(binder).addBinding().to(AnnotatedFilter.class);
		}

		@Provides
		private AnnotatedFilter provideFilter() {
			return new AnnotatedFilter();
		}
		
		@WebFilter(urlPatterns = "/b/*")
		class AnnotatedFilter implements Filter {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				// do nothing
			}

			@Override
			public void destroy() {
				// do nothing
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				assertion = () -> {
				};
			}
		}
	}

}
