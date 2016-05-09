package com.nhl.bootique.jetty.servlet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.unit.JettyApp;

public class AnnotatedFilter_ParamsIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test
	public void testAnnotationParams() throws Exception {
		app.startServer(new FilterModule());

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/b/").request().get();
		assertEquals("p1_v1_p2_v2", r.readEntity(String.class));
	}

	@Test
	public void testConfig_Override() throws Exception {

		app.startServer(new FilterModule(), "--config=classpath:com/nhl/bootique/jetty/servlet/AnnotatedFilterIT2.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/b/").request().get();
		assertEquals("p1_v3_p2_v4", r.readEntity(String.class));
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

		@WebFilter(filterName = "f1", urlPatterns = "/b/*", initParams = { @WebInitParam(name = "p1", value = "v1"),
				@WebInitParam(name = "p2", value = "v2") })
		class AnnotatedFilter implements Filter {

			private FilterConfig config;

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				this.config = filterConfig;
			}

			@Override
			public void destroy() {
				// do nothing
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				((HttpServletResponse) response).getWriter()
						.append("p1_" + config.getInitParameter("p1") + "_p2_" + config.getInitParameter("p2"));
			}
		}
	}

}
