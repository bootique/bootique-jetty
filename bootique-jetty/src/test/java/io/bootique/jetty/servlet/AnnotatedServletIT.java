package io.bootique.jetty.servlet;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.unit.JettyApp;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertNull;

public class AnnotatedServletIT {

	private Runnable assertion;

	@Rule
	public JettyApp app = new JettyApp();

	@After
	public void after() {
		assertion = null;
	}

	@Test
	public void testServletContainerState() throws Exception {
		app.start(new ServletModule());

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		assertNull(assertion);

		base.path("/b").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;

		base.path("/b/1").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;

		base.path("/b/2").request().get();
		Objects.requireNonNull(assertion).run();
	}

	@Test
	public void testConfig_Override() throws Exception {

		app.start(new ServletModule(),
				"--config=classpath:io/bootique/jetty/servlet/AnnotatedServletIT1.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		assertNull(assertion);

		base.path("/b").request().get();
		assertNull(assertion);

		base.path("/c").request().get();
		Objects.requireNonNull(assertion).run();
		assertion = null;
	}

	class ServletModule implements Module {

		@Override
		public void configure(Binder binder) {
			JettyModule.extend(binder).addServlet(AnnotatedServlet.class);
		}

		@Provides
		AnnotatedServlet createAnnotatedServlet() {
			return new AnnotatedServlet();
		}

		@WebServlet(name = "s1", urlPatterns = "/b/*")
		class AnnotatedServlet extends HttpServlet {

			private static final long serialVersionUID = -8896839263652092254L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertion = () -> {
				};
			}
		}
	}

}
