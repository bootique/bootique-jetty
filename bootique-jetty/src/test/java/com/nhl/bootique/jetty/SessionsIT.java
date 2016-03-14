package com.nhl.bootique.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Rule;
import org.junit.Test;

import com.nhl.bootique.jetty.unit.JettyApp;

public class SessionsIT {

	@Rule
	public JettyApp app = new JettyApp();

	@Test
	public void testSessions() {

		Map<String, String> params = new HashMap<>();
		params.put("a", "a1");
		params.put("b", "b2");
		MappedServlet mappedServlet = new MappedServlet(new TestServlet(), new HashSet<>(Arrays.asList("/*")), "s1");

		app.startWithArgs(binder -> JettyModule.contributeServlets(binder).addBinding().toInstance(mappedServlet),
				"--server");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("count: 1", r1.readEntity(String.class));
		NewCookie sessionId = r1.getCookies().get("JSESSIONID");
		
		assertNotNull(sessionId);

		Response r2 = base.path("/").request().cookie(sessionId).get();
		assertEquals(Status.OK.getStatusCode(), r2.getStatus());
		assertEquals("count: 2", r2.readEntity(String.class));
	}

	@Test
	public void testNoSessions() {

		Map<String, String> params = new HashMap<>();
		params.put("a", "a1");
		params.put("b", "b2");
		MappedServlet mappedServlet = new MappedServlet(new TestServlet(), new HashSet<>(Arrays.asList("/*")), "s1");

		app.startWithArgs(binder -> JettyModule.contributeServlets(binder).addBinding().toInstance(mappedServlet),
				"--server", "--config=src/test/resources/com/nhl/bootique/jetty/nosessions.yml");

		WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

		Response r1 = base.path("/").request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("nosessions", r1.readEntity(String.class));
	}

	static class TestServlet extends HttpServlet {
		private static final long serialVersionUID = -3190255883516320766L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

			String message;

			try {

				// this throws if sessions are disabled
				HttpSession session = req.getSession(true);

				Integer count = (Integer) session.getAttribute("count");
				count = count != null ? count + 1 : 1;
				session.setAttribute("count", count);
				message = "count: " + count;
			} catch (IllegalStateException e) {
				message = "nosessions";
			}

			resp.setContentType("text/plain");
			resp.getWriter().print(message);
		}
	}

}
