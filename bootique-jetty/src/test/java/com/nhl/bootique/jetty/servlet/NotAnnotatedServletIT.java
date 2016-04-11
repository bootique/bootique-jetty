package com.nhl.bootique.jetty.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.unit.JettyApp;

public class NotAnnotatedServletIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test(expected = RuntimeException.class)
	public void testServletContatinerState() throws Exception {
		app.startServer(new ServletModule());
	}

	class ServletModule implements Module {

		@Override
		public void configure(Binder binder) {
			JettyModule.contributeServlets(binder).addBinding().to(NotAnnotatedServlet.class);
		}

		@Provides
		NotAnnotatedServlet createAnnotatedServlet() {
			return new NotAnnotatedServlet();
		}

		class NotAnnotatedServlet extends HttpServlet {

			private static final long serialVersionUID = -8896839263652092254L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

			}
		}
	}

}
