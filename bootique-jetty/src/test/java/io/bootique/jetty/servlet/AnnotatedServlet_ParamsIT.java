package io.bootique.jetty.servlet;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.unit.JettyApp;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AnnotatedServlet_ParamsIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test
	public void testAnnotationParams() throws Exception {
		app.start(new ServletModule());

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/b").request().get();
		assertEquals("p1_v1_p2_v2", r.readEntity(String.class));
	}

	@Test
	public void testConfig_Override() throws Exception {

		app.start(new ServletModule(),
				"--config=classpath:io/bootique/jetty/servlet/AnnotatedServletIT2.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r = base.path("/b").request().get();
		assertEquals("p1_v3_p2_v4", r.readEntity(String.class));
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

		@WebServlet(name = "s1", urlPatterns = "/b/*", initParams = { @WebInitParam(name = "p1", value = "v1"),
				@WebInitParam(name = "p2", value = "v2") })
		class AnnotatedServlet extends HttpServlet {

			private static final long serialVersionUID = -8896839263652092254L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.getWriter().append("p1_" + getInitParameter("p1") + "_p2_" + getInitParameter("p2"));
			}
		}
	}

}
