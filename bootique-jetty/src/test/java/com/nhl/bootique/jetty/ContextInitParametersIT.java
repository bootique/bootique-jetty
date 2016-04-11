package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Rule;
import org.junit.Test;

import com.nhl.bootique.jetty.unit.JettyApp;

public class ContextInitParametersIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test
	public void testInitParametersPassed() {

		Map<String, String> params = new HashMap<>();
		params.put("a", "a1");
		params.put("b", "b2");
		MappedServlet mappedServlet = new MappedServlet(new TestServlet(), new HashSet<>(Arrays.asList("/*")), "s1");

		app.startServer(binder -> JettyModule.contributeMappedServlets(binder).addBinding().toInstance(mappedServlet),
				"--config=src/test/resources/com/nhl/bootique/jetty/ContextInitParametersIT.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());

		assertEquals("s1_a1_b2", r1.readEntity(String.class));
	}

	static class TestServlet extends HttpServlet {
		private static final long serialVersionUID = -3190255883516320766L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");

			ServletConfig config = getServletConfig();

			resp.getWriter().print(config.getServletName());
			resp.getWriter().print("_" + config.getServletContext().getInitParameter("a"));
			resp.getWriter().print("_" + config.getServletContext().getInitParameter("b"));
		}
	}
}
