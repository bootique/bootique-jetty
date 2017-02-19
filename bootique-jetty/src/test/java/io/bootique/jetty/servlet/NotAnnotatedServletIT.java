package io.bootique.jetty.servlet;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.unit.JettyApp;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class NotAnnotatedServletIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test(expected = RuntimeException.class)
	public void testServletContatinerState() throws Exception {
		app.start(new ServletModule());
	}

	class ServletModule implements Module {

		@Override
		public void configure(Binder binder) {
			JettyModule.extend(binder).addServlet(NotAnnotatedServlet.class);
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
