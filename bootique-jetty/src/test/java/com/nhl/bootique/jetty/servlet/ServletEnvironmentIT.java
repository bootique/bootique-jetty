package com.nhl.bootique.jetty.servlet;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.MappedServlet;
import com.nhl.bootique.jetty.unit.JettyApp;

public class ServletEnvironmentIT {

	private Runnable assertion;

	@Rule
	public JettyApp app = new JettyApp();

	@After
	public void after() {
		assertion = null;
	}

	@Test
	public void testServletContatinerState() throws Exception {
		app.start(new ServletCheckingModule());

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		assertNull(assertion);

		base.path("/a").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;

		base.path("/a/1").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;

		base.path("/a/2").request().get();
		Objects.requireNonNull(assertion).run();
	}

	class ServletCheckingModule implements Module {

		@Override
		public void configure(Binder binder) {
			JettyModule.contributeServlets(binder).addBinding().to(MappedServlet.class);
		}

		@Provides
		MappedServlet createMappedServlet(ServletCheckingState servlet) {
			return new MappedServlet(servlet, new HashSet<>(Arrays.asList("/a/*")));
		}

		@Provides
		ServletCheckingState createServlet(ServletEnvironment state) {
			return new ServletCheckingState(state);
		}

		class ServletCheckingState extends HttpServlet {

			private static final long serialVersionUID = -1713490500665580905L;
			private ServletEnvironment state;

			public ServletCheckingState(ServletEnvironment state) {
				this.state = state;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// capture variable values before sticking them in the
				// closure...
				HttpServletRequest stateRequest = state.request().get();
				ServletContext stateContext = state.context().get();
				ServletContext requestContext = req.getServletContext();

				assertion = () -> {
					assertSame(req, stateRequest);
					assertSame(requestContext, stateContext);
				};
			}
		}
	}

}
