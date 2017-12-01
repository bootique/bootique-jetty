package io.bootique.jetty;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ServletInitParametersIT {

	@Rule
	public BQTestFactory testFactory = new BQTestFactory();

	@Test
	public void testInitParametersPassed() {

		testFactory.app("-s", "-c", "classpath:io/bootique/jetty/ServletInitParametersIT.yml")
				.autoLoadModules()
				.module(b -> JettyModule.extend(b).addServlet(new TestServlet(), "s1", "/*"))
				.run();

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		assertEquals("s1_a1_b2", r1.readEntity(String.class));
	}

	static class TestServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");

			ServletConfig config = getServletConfig();

			resp.getWriter().print(config.getServletName());
			resp.getWriter().print("_" + config.getInitParameter("a"));
			resp.getWriter().print("_" + config.getInitParameter("b"));
		}
	}
}
